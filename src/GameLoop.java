import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.io.IOException;

public abstract class GameLoop {
    Stage stage;
    Scene scene;
    Group root;
    Group boundsGroup;
    Group componentsGroup;

    ImageView arrow;
    ImageView arrowRed;

    private int sceneNum;

    private double stageWidth;
    private double stageHeight;

    Player player;

    private boolean rightPressed;
    private boolean leftPressed;
    private boolean ePressed;

    boolean canInteract;
    boolean canExit;

    private boolean hasArrow;
    private boolean hasArrowRed;
    private boolean nearDoor;
    private boolean nearMicrowave;

    boolean enteredDoor;

    private int flowSceneNum;


    public GameLoop(Stage primaryStage, boolean scrollable, int sceneNum) {
        primaryStage.setOnCloseRequest(e -> InterconnectedIsolation.closeProgram());
        stage = primaryStage;
        root = new Group();
        scene = new Scene(root, 1280, 720, Color.BLACK);
        componentsGroup = new Group();
        boundsGroup = new Group();
        root.getChildren().addAll(componentsGroup, boundsGroup);

        scene.setOnKeyPressed(onPressHandler);
        scene.setOnKeyReleased(onReleaseHandler);
        scene.setOnMouseClicked(onMouseHandler);

        this.sceneNum = sceneNum;
        try {
            arrow = new ImageView(new Image(new FileInputStream("assets/images/arrow.png")));
            arrowRed = new ImageView(new Image(new FileInputStream("assets/images/arrow_red.png")));
            initBackground(sceneNum);
        } catch (IOException e) {
        }
        arrow.setFitWidth(40);
        arrow.setPreserveRatio(true);
        arrowRed.setFitWidth(40);
        arrowRed.setPreserveRatio(true);

        Rectangle rightBounds = new Rectangle();
        rightBounds.setHeight(scene.getHeight());
        rightBounds.setWidth(5);
        rightBounds.setTranslateX(scene.getWidth());

        Rectangle leftBounds = new Rectangle();
        leftBounds.setHeight(scene.getHeight());
        leftBounds.setWidth(5);
        leftBounds.setTranslateX(-5);

        boundsGroup.getChildren().addAll(leftBounds, rightBounds);

        initStage();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (scrollable) {
                    if (Math.floor(player.playerView.getBoundsInParent().getMaxX()) >= Math.floor(rightBounds.getBoundsInParent().getMinX() - componentsGroup.getTranslateX() - 570)) {
                        if (Math.floor(componentsGroup.getTranslateX()) >= -(stageWidth - 1280))
                            componentsGroup.setTranslateX(Math.floor(componentsGroup.getTranslateX() - player.getSpeed()));
                    } else if (Math.floor(player.playerView.getBoundsInParent().getMinX()) <= Math.floor(leftBounds.getBoundsInParent().getMaxX() - componentsGroup.getTranslateX() + 570)) {
                        if (Math.floor(componentsGroup.getTranslateX()) <= -5)
                            componentsGroup.setTranslateX(Math.floor(componentsGroup.getTranslateX() + player.getSpeed()));
                    }
                }
                checkForCollisions();

                if (checkForInteraction() && canInteract) {
                    if (!hasArrow) {
                        componentsGroup.getChildren().add(arrow);
                        hasArrow = true;
                    }
                    if (nearMicrowave && ePressed) {
                        System.out.println("microwave");
                    }
                } else {
                    componentsGroup.getChildren().remove(arrow);
                    hasArrow = false;
                }

                if (checkForDoor() && canExit) {
                    if (!hasArrowRed) {
                        componentsGroup.getChildren().add(arrowRed);
                        hasArrowRed = true;
                    }
                    if (ePressed) {
                        enteredDoor = true;
                        stop();
                        flowSceneNum++;
                        if (flowSceneNum == 1)
                            new Level1(InterconnectedIsolation.window, 2298, 720, 1).display();
                    }
                } else {
                    componentsGroup.getChildren().remove(arrowRed);
                    hasArrowRed = false;
                }

                if (rightPressed && player.getCanMoveRight()) {
                    player.playerView.setImage(player.getPlayerRight());
                    player.moveRight();
                } else if (leftPressed && player.getCanMoveLeft()) {
                    player.playerView.setImage(player.getPlayerLeft());
                    player.moveLeft();
                } else player.playerView.setImage(player.getPlayerStand());
            }
        }.start();
    }

    private void checkForCollisions() {
        if (checkForCollisionOnLeft()) {
            player.setCanMoveLeft(false);
        } else {
            player.setCanMoveLeft(true);
        }
        if (checkForCollisionOnRight()) {
            player.setCanMoveRight(false);
        } else {
            player.setCanMoveRight(true);
        }
    }

    public boolean checkForCollisionOnRight() {
        if (player.getMaxX() >= stageWidth - 20)
            return true;
        return false;
    }

    public boolean checkForCollisionOnLeft() {
        if (player.getMinX() <= 120)
            return true;
        return false;
    }

    public boolean checkForInteraction() {
        if (sceneNum == 2) {
            if (player.getAverageX() > 510 && player.getAverageX() < 670) {
                nearMicrowave = true;
                arrow.setX(580);
                arrow.setY(100);
            } else {
                nearMicrowave = false;
            }
        }
        return nearMicrowave;
    }

    public boolean checkForDoor() {
        if (sceneNum == 2) {
            if (player.getAverageX() > 2020 && player.getAverageX() < 2220) {
                nearDoor = true;
                arrowRed.setX(2100);
                arrowRed.setY(190);
            } else {
                nearDoor = false;
            }
        }
        return nearDoor;
    }

    public abstract void initStage();

    public abstract void initBackground(int sceneNum) throws IOException;

    EventHandler onPressHandler = new EventHandler<KeyEvent>() {
        @Override
        public void handle(KeyEvent event) {
            switch (event.getCode()) {
                case D:
                    rightPressed = true;
                    break;
                case A:
                    leftPressed = true;
                    break;
                case E:
                    ePressed = true;
                    break;

            }
        }
    };

    EventHandler onMouseHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            int x = (int) (event.getSceneX() - componentsGroup.getTranslateX()), y = (int) event.getSceneY();
            if (x >= 485 && x <= 726 && y >= 169 && y <= 278)
                System.out.println("Clicked on the microwave!");
            else if (x >= player.playerView.getTranslateX() && x <= player.playerView.getTranslateX() + 39)
                System.out.println("Clicked on player!");
        }
    };
    EventHandler onReleaseHandler = new EventHandler<KeyEvent>() {
        @Override
        public void handle(KeyEvent event) {
            switch (event.getCode()) {
                case D:
                    rightPressed = false;
                    break;
                case A:
                    leftPressed = false;
                    break;
                case E:
                    ePressed = false;
                    break;
            }
        }
    };

    public void setStageWidth(int width) {
        stageWidth = width;
    }

    public void setStageHeight(double stageHeight) {
        this.stageHeight = stageHeight;
    }

    public void display() {
        FadeTransition fade = new FadeTransition(Duration.millis(4000), componentsGroup);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        stage.setScene(scene);
        // stage.centerOnScreen();
    }
}
