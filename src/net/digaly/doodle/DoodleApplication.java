package net.digaly.doodle;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import net.digaly.doodle.audio.SoundManager;
import net.digaly.doodle.events.EventDispatcher;
import net.digaly.doodle.events.FrameDrawListener;
import net.digaly.doodle.events.KeyState;
import net.digaly.doodle.events.MouseEventListener;

import java.util.Hashtable;

/**
 * Created by Tom Dobbelaere on 1/10/2016.
 */
public class DoodleApplication extends Application
{
    private static DoodleApplication instance;

    private Room currentRoom;
    private Hashtable<KeyCode, KeyEvent> heldKeys;

    private EventDispatcher eventDispatcher;
    private SoundManager soundManager;

    private double lastT;
    private int fpsCounter;
    private int currentFPS;
    private String title;
    private boolean fullscreen;
    private String icon;

    private Effect renderEffect;

    public static DoodleApplication getInstance() {
        if (instance == null) {
            instance = new DoodleApplication();
            instance.soundManager = new SoundManager();
            instance.eventDispatcher = new EventDispatcher();
            instance.heldKeys = new Hashtable<>();
            instance.fullscreen = false;
            instance.renderEffect = null;
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

        GraphicsContext gc = canvas.getGraphicsContext2D();
        final long startNanoTime = System.nanoTime();

        mainScene.setOnMouseClicked(event -> instance.broadcastMouseEvent(event));
        mainScene.setOnMousePressed(event -> instance.broadcastMouseEvent(event));
        mainScene.setOnMouseReleased(event -> instance.broadcastMouseEvent(event));

        mainScene.setOnKeyPressed(event -> {
            instance.heldKeys.put(event.getCode(), event);
            instance.eventDispatcher.notifyKeyEventListeners(event, KeyState.PRESSED);
        });

        mainScene.setOnKeyReleased(event -> {
            instance.heldKeys.remove(event.getCode());
            instance.eventDispatcher.notifyKeyEventListeners(event, KeyState.RELEASED);
        });

        new AnimationTimer() {
            @Override
            public void handle(long now)
            {
                double t = (now - startNanoTime) / 1000000000.0;

                instance.onFrame(t, gc);
            }
        }.start();

        instance.eventDispatcher.notifyApplicationReadyListener();
    }

    private void onFrame(double t, GraphicsContext gc) {
        instance.eventDispatcher.notifyFrameUpdateListeners();

        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        for (KeyCode key : instance.heldKeys.keySet()) {
            instance.eventDispatcher.notifyKeyEventListeners(instance.heldKeys.get(key), KeyState.HOLDING);
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

        /*gc.setFill(Color.YELLOW);
        gc.setFont(new Font(24));
        gc.fillText("Sound pool: " + instance.getSoundManager() .size(), 0, gc.getCanvas().getHeight() - 48);*/

        /*gc.setFill(Color.VIOLET);
        gc.setFont(new Font(24));
        gc.fillText("FU (" + frameUpdateListeners.size() + "), FD (" + frameDrawListeners.size() + ")", 0, gc.getCanvas().getHeight() - 72);*/

        if (t - lastT > 1) {
            lastT = t;
            currentFPS = fpsCounter;
            fpsCounter = 0;
        }

        fpsCounter += 1;

        if (instance.renderEffect != null) gc.applyEffect(instance.renderEffect);
    }

    private void broadcastMouseEvent(MouseEvent event) {
        instance.eventDispatcher.notifyMouseEventListeners(event, false);

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

    public EventDispatcher getEventDispatcher()
    {
        return eventDispatcher;
    }

    public SoundManager getSoundManager()
    {
        return soundManager;
    }

    public void setRenderEffect(Effect renderEffect)
    {
        this.renderEffect = renderEffect;
    }
}
