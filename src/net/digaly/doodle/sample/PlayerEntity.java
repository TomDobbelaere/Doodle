package net.digaly.doodle.sample;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import net.digaly.doodle.DoodleApplication;
import net.digaly.doodle.Entity;
import net.digaly.doodle.Sprite;
import net.digaly.doodle.events.FrameUpdateListener;
import net.digaly.doodle.events.KeyEventListener;
import net.digaly.doodle.events.KeyState;
import net.digaly.doodle.events.MouseEventListener;

import java.util.Random;

import static javafx.scene.input.KeyCode.Z;

/**
 * Created by Tom Dobbelaere on 2/10/2016.
 */
public class PlayerEntity extends Entity implements FrameUpdateListener, KeyEventListener, MouseEventListener
{
    private double speed;
    private int turnSpeed = 5;

    private Sprite spriteForwards;
    private Sprite spriteBackwards;
    private Sprite spriteNone;
    private Sprite spriteBullet;

    private Random random;

    private int shootDelay ;

    public PlayerEntity(double x, double y)
    {
        super(new Sprite("ship_n.png"), x, y);

        setDepth(100);

        spriteNone = new Sprite("ship_n.png");
        spriteForwards = new Sprite("ship_f.png");
        spriteBackwards = new Sprite("ship_b.png");
        spriteBullet = new Sprite("bullet.png");
        random = new Random();

        shootDelay = 0;
        speed = 0;
    }

    @Override
    public void onFrameUpdate()
    {
        getPosition().translate(Math.cos(getAngle() * 0.017) * speed, Math.sin(getAngle() * 0.017) * speed);

        if (Math.abs(speed) > 5)
        {
            if (speed > 0) speed = 5;
            if (speed < 0) speed = -5;
        }

        if (speed > 0) speed -= 0.1;
        if (speed < 0) speed += 0.1;

        turnSpeed = 5 - (int) speed / 2;

        DoodleApplication.getInstance().getCurrentRoom().addEntity(new TrailEntity(getSprite(), getPosition().x, getPosition().y, getAngle(), 0.2, 0.01));

        setSprite(spriteNone);

        if (shootDelay > 0) shootDelay -= 1;

        //gc.fillOval(getPosition().x, getPosition().y, 32, 32);
    }

    @Override
    public void onKeyEvent(KeyEvent keyEvent, KeyState keyState)
    {
        if (keyState == KeyState.HOLDING) {
            switch (keyEvent.getCode()) {
                case Z:
                    speed += 0.2;
                    setSprite(spriteForwards);
                    break;
                case S:
                    speed -= 0.2;
                    setSprite(spriteBackwards);
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
                DoodleApplication.getInstance().setCurrentRoom(new LevelOtherRoom());
            }
        }
    }

    private void shoot() {
        if (shootDelay == 0) {
            int angleSwing = -5 + random.nextInt(10);

            DoodleApplication.getInstance().getCurrentRoom().addEntity(new BulletEntity(spriteBullet, getPosition().x + 20, getPosition().y + 32, getAngle()+ angleSwing, 10));

            angleSwing = -5 + random.nextInt(10);

            DoodleApplication.getInstance().getCurrentRoom().addEntity(new BulletEntity(spriteBullet, getPosition().x + 20, getPosition().y + 32, getAngle()+ angleSwing, 10));
            shootDelay = 5;
            DoodleApplication.getInstance().getSoundManager().playSound("res\\shoot.wav");
        }
    }

    @Override
    public void onMouseEvent(MouseEvent event, boolean isLocal)
    {
        if (event.getEventType() == MouseEvent.MOUSE_MOVED || event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            double deltaX = getPosition().x + getSprite().getOffset().x - event.getSceneX();
            double deltaY = getPosition().y + getSprite().getOffset().y - event.getSceneY();
            double angle = Math.atan2(deltaY, deltaX) * 180 / Math.PI;

            setAngle((int) angle + 180);
        }

        if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            shoot();
        }

        if (isLocal && event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            setVisible(!isVisible());
        }
    }
}
