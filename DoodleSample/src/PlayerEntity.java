import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import net.digaly.doodle.AnimatedSprite;
import net.digaly.doodle.DoodleApplication;
import net.digaly.doodle.Entity;
import net.digaly.doodle.Sprite;
import net.digaly.doodle.collision.BoxCollider;
import net.digaly.doodle.events.*;

import java.util.Random;

import static javafx.scene.input.KeyCode.Z;

/**
 * Created by Tom Dobbelaere on 2/10/2016.
 */
public class PlayerEntity extends Entity implements FrameUpdateListener, KeyEventListener, MouseEventListener, CollisionEventListener, FrameDrawListener
{
    private double speed;
    private int turnSpeed = 5;

    private Sprite spriteNone;
    private Sprite spriteFlying;
    private Sprite spriteBullet;

    private Random random;

    private int shootDelay;
    private int bulletPower;
    private double bulletSpread;

    public PlayerEntity(double x, double y)
    {
        super(new Sprite("doodle\\ship_normal.png"), 0, 0);

        setDepth(10);
        setCollider(new BoxCollider(this, getSprite().getOffset().x - 16, getSprite().getOffset().y - 16, 32, 32));

        //spriteNone = new Sprite("ship_n.png");
        spriteNone = getSprite();
        spriteFlying = new Sprite("doodle\\ship_normal.png");//new AnimatedSprite(new String[] {"v2\\ship_flyingA.png", "v2\\ship_flyingB.png"}, 5);
        spriteBullet = new Sprite("doodle\\bullet.png");
        random = new Random();

        shootDelay = 0;
        speed = 0;
        bulletPower = 2;
        bulletSpread = 10;
    }

    @Override
    public void onFrameUpdate()
    {
        Camera camera = getRoom().getRenderer().getCamera();
        Group root = getRoom().getRenderer().getRoot();

        double camTargetX = getPosition().x - root.getScene().getWidth() / 2;
        double camTargetY = getPosition().y - root.getScene().getHeight() / 2;

        if (camTargetX > 0 && camTargetX < getRoom().getSize().getWidth() - root.getScene().getWidth()) {
            camera.setTranslateX(camTargetX);
        }

        if (camTargetY > 0 && camTargetY < getRoom().getSize().getHeight() - root.getScene().getHeight()) {
            camera.setTranslateY(camTargetY);
        }

        double targetTranslateX = Math.cos(getAngle() * 0.017) * speed;
        double targetTranslateY = Math.sin(getAngle() * 0.017) * speed;

        if (getPosition().x + targetTranslateX > 0 && getPosition().x + targetTranslateX < getRoom().getSize().getWidth()) {
            getPosition().translate(Math.cos(getAngle() * 0.017) * speed, 0);
        }

        if (getPosition().y + targetTranslateY > 0 && getPosition().y + targetTranslateY < getRoom().getSize().getHeight()) {
            getPosition().translate(0, Math.sin(getAngle() * 0.017) * speed);
        }

        //Speed limiting
        if (speed > 6)
        {
            speed = 6;
        }

        if (speed < -5) {
            speed = -5;
        }

        //Slowing down
        if (speed > 0) speed -= 0.1;
        if (speed < 0) speed += 0.1;

        if (Math.abs(speed) < 0.1) speed = 0;

        turnSpeed = 5 - (int) speed / 2;

        //getRoom().addEntity(new TrailEntity(getSprite(), getPosition().x, getPosition().y, getAngle(), 0.2, 0.01));

        setSprite(spriteNone);

        if (shootDelay > 0) shootDelay -= 1;
    }

    @Override
    public void onKeyEvent(KeyEvent keyEvent, KeyState keyState)
    {
        if (keyState == KeyState.HOLDING) {
            switch (keyEvent.getCode()) {
                case Z:
                    speed += 0.2;
                    setSprite(spriteFlying);
                    break;
                case S:
                    speed -= 0.2;
                    setSprite(spriteFlying);
                    break;
                case Q:
                    setAngle(getAngle() - turnSpeed);
                    break;
                case D:
                    setAngle(getAngle() + turnSpeed);
                    break;
                case E:
                    shoot();
                    break;
            }
        }

        if (keyState == KeyState.RELEASED) {
            if (keyEvent.getCode() == KeyCode.R) {
                //DoodleApplication.getInstance().setCurrentRoom(new LevelOtherRoom());
            }
        }
    }

    private void shoot() {
        if (shootDelay == 0) {
            int angleSwing;

            for (int i = 0; i < bulletPower; i++) {
                angleSwing = (int) -(bulletSpread/2) + random.nextInt((int) bulletSpread);
                getRoom().addEntity(new BulletEntity(spriteBullet, getPosition().x, getPosition().y, getAngle()+ angleSwing, 10));
            }

            shootDelay = 5;
            getRoom().getSoundManager().playSound("DoodleSample\\res\\shoot.wav");
        }
    }

    @Override
    public void onMouseEvent(MouseEvent event, MouseState state, boolean isLocal)
    {
        if (event.getEventType() == MouseEvent.MOUSE_MOVED || event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            double deltaX = getPosition().x - event.getX();
            double deltaY = getPosition().y - event.getY();
            double angle = Math.atan2(deltaY, deltaX) * 180 / Math.PI;

            setAngle((int) angle + 180);
        }

        if (event.getButton() == MouseButton.PRIMARY && state == MouseState.HOLDING) {
            shoot();
        }
    }

    @Override
    public void onFrameDraw(GraphicsContext gc)
    {
        gc.save();
        gc.setGlobalBlendMode(BlendMode.HARD_LIGHT);
        gc.drawImage(getSprite().getImage(), 0, 0);
        gc.restore();
    }

    @Override
    public void onCollisionStay(Entity other)
    {
        if (other instanceof GeomEntity) {
            if (((GeomEntity) other).canPickup()) {
                //Geom collected
                getRoom().getSoundManager().playSound("DoodleSample\\res\\collect.wav", 0.3);
                other.destroy();
            }
        }
    }

    @Override
    public void onCollisionEnter(Entity other)
    {

    }

    @Override
    public void onCollisionExit(Entity other)
    {

    }

/*
    @Override
    public void onFrameDraw(GraphicsContext gc)
    {
        gc.setFill(Color.RED);
        gc.fillOval(0, 0, 200, 200);
    }*/
}
