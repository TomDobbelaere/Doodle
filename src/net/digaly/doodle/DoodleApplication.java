package net.digaly.doodle;

import com.sun.corba.se.impl.orbutil.graph.Graph;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.binding.DoubleBinding;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Tom Dobbelaere on 1/10/2016.
 */
public class DoodleApplication extends Application
{
    private static DoodleApplication instance;
    private Room currentRoom;
    private List<FrameUpdateListener> frameUpdateListeners;
    private List<FrameDrawListener> frameDrawListeners;
    private List<KeyEventListener> keyEventListeners;
    private List<MouseEventListener> mouseEventListeners;
    private List<ApplicationReadyListener> applicationReadyListeners;
    private HashMap<KeyCode, KeyEvent> heldKeys;
    private GraphicsContext gc;
    private MediaPlayer musicPlayer;
    private double lastT;
    private int fpsCounter;
    private int currentFPS;
    private List<MediaPlayer> soundPool;
    private String title;
    private boolean fullscreen;
    private String icon;

    public static DoodleApplication getInstance() {
        if (instance == null) {
            instance = new DoodleApplication();
            instance.frameUpdateListeners = new CopyOnWriteArrayList<>();
            instance.keyEventListeners = new CopyOnWriteArrayList<>();
            instance.applicationReadyListeners = new CopyOnWriteArrayList<>();
            instance.frameDrawListeners = new CopyOnWriteArrayList<>();
            instance.mouseEventListeners = new CopyOnWriteArrayList<>();
            instance.soundPool = new CopyOnWriteArrayList<>();
            instance.heldKeys = new HashMap<>();
            instance.fullscreen = false;
        }

        return instance;
    }

    public void run() {
        launch();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setFullscreen(boolean value) {
        this.fullscreen = value;
    }

    public void setIcon(String filename) {
        this.icon = filename;
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        if (instance.icon != null) primaryStage.getIcons().add(new Image(instance.icon));
        primaryStage.setFullScreen(instance.fullscreen);
        primaryStage.setTitle(instance.title);

        Group root = new Group();
        Scene mainScene = new Scene(root);
        mainScene.setFill(Color.BLACK);
        primaryStage.setScene(mainScene);


        Canvas canvas = new Canvas(instance.currentRoom.getSize().getWidth(), instance.currentRoom.getSize().getHeight());
        root.getChildren().add(canvas);
        primaryStage.show();

        if (instance.fullscreen)
        {
            double scaleTarget = primaryStage.getHeight() / canvas.getHeight();
            Scale scale = new Scale();
            scale.setX(scaleTarget);
            scale.setY(scaleTarget);
            scale.setPivotX(0);
            scale.setPivotY(0);
            canvas.getTransforms().add(scale);
            canvas.setLayoutX(canvas.getWidth() / scaleTarget / 2);
        }

        //canvas.widthProperty().bind(mainScene.widthProperty());
        //canvas.heightProperty().bind(mainScene.heightProperty());

        instance.gc = canvas.getGraphicsContext2D();
        final long startNanoTime = System.nanoTime();

        mainScene.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                instance.broadcastMouseEvent(event);
            }
        });

        mainScene.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                instance.broadcastMouseEvent(event);
            }
        });

        mainScene.setOnMouseReleased(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                instance.broadcastMouseEvent(event);
            }
        });

        mainScene.setOnKeyPressed(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
            {
                instance.heldKeys.put(event.getCode(), event);

                instance.notifyKeyEventListeners(event, KeyState.PRESSED);
            }
        });

        mainScene.setOnKeyReleased(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
            {
                instance.heldKeys.remove(event.getCode());

                instance.notifyKeyEventListeners(event, KeyState.RELEASED);
            }
        });

        new AnimationTimer() {
            @Override
            public void handle(long now)
            {
                double t = (now - startNanoTime) / 1000000000.0;

                instance.onFrame(t);
            }
        }.start();

        notifyApplicationReadyListener();
    }

    private void onFrame(double t) {
        notifyFrameUpdateListeners();

        Bloom bloom = new Bloom();
        gc.setEffect(bloom);

        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        for (KeyCode key : instance.heldKeys.keySet()) {
            notifyKeyEventListeners(instance.heldKeys.get(key), KeyState.HOLDING);
        }

        if (!(getCurrentRoom() instanceof FrameDrawListener)) {
            gc.drawImage(getCurrentRoom().getBackground().getImage(), 0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        } else {
            ((FrameDrawListener) getCurrentRoom()).onFrameDraw(gc);
        }

        for (Entity entity : instance.getCurrentRoom().getEntities()) {
            if (!entity.isVisible()) continue;

            gc.save();
            gc.setGlobalAlpha(entity.getAlpha());

            if (!(entity instanceof FrameDrawListener)) {
                entity.draw(gc);
            } else {
                ((FrameDrawListener) entity).onFrameDraw(gc);
            }

            gc.restore();
        }

        gc.setFill(Color.LIMEGREEN);
        gc.setFont(new Font(24));
        gc.fillText("FPS: " + String.valueOf(currentFPS), 0, gc.getCanvas().getHeight());

        gc.setFill(Color.AQUA);
        gc.setFont(new Font(24));
        gc.fillText("Entities: " + getCurrentRoom().getEntities().size(), 0, gc.getCanvas().getHeight() - 24);

        gc.setFill(Color.YELLOW);
        gc.setFont(new Font(24));
        gc.fillText("Sound pool: " + instance.soundPool.size(), 0, gc.getCanvas().getHeight() - 48);

        if (t - lastT > 1) {
            lastT = t;
            currentFPS = fpsCounter;
            fpsCounter = 0;
        }

        fpsCounter += 1;
    }

    private void broadcastMouseEvent(MouseEvent event) {
        notifyMouseEventListeners(event, false);

        for (Entity entity : instance.getCurrentRoom().getEntities()) {
            if (!(entity instanceof MouseEventListener)) continue;

            if (event.getSceneX() >= entity.getPosition().x && event.getSceneX() <= entity.getPosition().x + entity.getSprite().getImage().getWidth()
                    && event.getSceneY() >= entity.getPosition().y  && event.getSceneY() <= entity.getPosition().y + entity.getSprite().getImage().getHeight())
            {
                ((MouseEventListener) entity).onMouseEvent(event, true);
            }
        }
    }

    public Room getCurrentRoom()
    {
        return instance.currentRoom;
    }

    public void setCurrentRoom(Room room)
    {
        instance.currentRoom = room;
    }

    public void addFrameUpdateListener(FrameUpdateListener listener) {
        instance.frameUpdateListeners.add(listener);
    }

    public void removeFrameUpdateListener(FrameUpdateListener listener) {
        instance.frameUpdateListeners.remove(listener);
    }

    private void notifyFrameUpdateListeners() {
        for (FrameUpdateListener listener : instance.frameUpdateListeners) {
            listener.onFrameUpdate();
        }
    }

    public void addKeyEventListener(KeyEventListener listener) {
        instance.keyEventListeners.add(listener);
    }

    public void removeKeyEventListener(KeyEventListener listener) {
        instance.keyEventListeners.remove(listener);
    }

    private void notifyKeyEventListeners(KeyEvent keyEvent, KeyState keyState) {
        for (KeyEventListener listener : instance.keyEventListeners) {
            listener.onKeyEvent(keyEvent, keyState);
        }
    }

    public void addApplicationReadyListener(ApplicationReadyListener listener) {
        instance.applicationReadyListeners.add(listener);
    }

    public void removeApplicationReadyListener(ApplicationReadyListener listener) {
        instance.applicationReadyListeners.remove(listener);
    }

    private void notifyApplicationReadyListener() {
        for (ApplicationReadyListener listener : instance.applicationReadyListeners) {
            listener.onApplicationReady();
        }
    }

    public void addFrameDrawListener(FrameDrawListener listener) {
        instance.frameDrawListeners.add(listener);
    }

    public void removeFrameDrawListener(FrameDrawListener listener) {
        instance.frameDrawListeners.remove(listener);
    }

    private void notifyFrameDrawListeners(GraphicsContext passedgc) {
        for (FrameDrawListener listener : instance.frameDrawListeners) {
            listener.onFrameDraw(passedgc);
        }
    }

    public void addMouseEventListener(MouseEventListener listener) {
        instance.mouseEventListeners.add(listener);
    }

    public void removeMouseEventListener(MouseEventListener listener) {
        instance.mouseEventListeners.remove(listener);
    }

    private void notifyMouseEventListeners(MouseEvent event, boolean isLocal) {
        for (MouseEventListener listener : instance.mouseEventListeners) {
            listener.onMouseEvent(event, isLocal);
        }
    }

    public void playMusic(String filename) {
        Media media = new Media(new File(filename).toURI().toString()); //replace /Movies/test.mp3 with your file

        if (instance.musicPlayer != null) {
            instance.musicPlayer.stop();
        }

        instance.musicPlayer = new MediaPlayer(media);
        instance.musicPlayer.play();
        instance.musicPlayer.setOnEndOfMedia(new Runnable()
        {
            @Override
            public void run()
            {
                instance.musicPlayer.seek(Duration.ZERO);
            }
        });
    }

    public void setMusicVolume(double volume) {
        instance.musicPlayer.setVolume(volume);
    }

    public void playSound(String filename) {

        Media media = new Media(new File(filename).toURI().toString()); //replace /Movies/test.mp3 with your file
        MediaPlayer m = new MediaPlayer(media);
        soundPool.add(m);
        m.setOnEndOfMedia(new Runnable()
        {
            @Override
            public void run()
            {
                soundPool.remove(m);
            }
        });
        m.play();
    }
}
