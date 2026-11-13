package dev.ultreon.bubbles.ammo;

import com.badlogic.gdx.math.Shape2D;
import com.google.common.annotations.Beta;
import dev.ultreon.bubbles.entity.Bullet;
import dev.ultreon.bubbles.entity.Entity;
import dev.ultreon.bubbles.entity.attribute.AttributeContainer;
import dev.ultreon.bubbles.item.ItemType;
import dev.ultreon.bubbles.render.Renderer;


/**
 * Ammo type, handles things for {@link Bullet}.
 * This also adds a new {@link ItemType} to the registry.
 *
 * @author XyperCode
 */
public abstract class AmmoType {
    public abstract void render(Renderer renderer, Bullet bullet);

    /**
     * Handles collision for the given bullet if the bullet has {@link AmmoType this} as ammo type.
     *
     * @param bullet    bullet entity.
     * @param collided  the other entity that the bullet collided with.
     * @param deltaTime the collision delta time.
     */
    public void onCollision(Bullet bullet, Entity collided, double deltaTime) {

    }

    public abstract AttributeContainer getDefaultAttributes();

    public abstract Shape2D getShape(Bullet bullet);

    public abstract float getSpeed();
}
