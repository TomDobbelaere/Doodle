package net.digaly.doodle;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tom Dobbelaere on 2/10/2016.
 */
public class Room
{
    private List<Entity> entities;
    private Dimension size;

    public Room(int width, int height) {
        this.entities = new ArrayList<>();
        this.size = new Dimension(width, height);
    }

    public void addEntity(Entity entity) {
        this.entities.add(entity);

        if (entity instanceof FrameUpdateListener) {
            DoodleApplication.getInstance().addFrameUpdateListener((FrameUpdateListener) entity);
        }

        if (entity instanceof KeyEventListener) {
            DoodleApplication.getInstance().addKeyEventListener((KeyEventListener) entity);
        }
    }

    protected List<Entity> getEntities() {
        return this.entities;
    }

    public Dimension getSize() {
        return this.size;
    }
}
