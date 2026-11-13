package dev.ultreon.bubbles.ammo;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Shape2D;
import dev.ultreon.bubbles.entity.Bullet;
import dev.ultreon.bubbles.entity.attribute.AttributeContainer;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderer;

public class RocketAmmoType extends AmmoType {
    @Override
    public void render(Renderer renderer, Bullet bullet) {
        renderer.fill(this.getShape(bullet), Colors.rgb(0xffdf00));
    }

    @Override
    public AttributeContainer getDefaultAttributes() {
        return null;
    }

    /**
     *    #
     *   # #
     *   # #
     *  # # #
     * ### ###
     */
    @Override
    public Shape2D getShape(Bullet bullet) {
        return new Polygon(new float[]{
                0, 2,
                1, 1,
                1, 0,
                2, -1,
                4, -3,
                1, -3,
                0, -2,
                -1, -3,
                -4, -3,
                -2, -1,
                -1, 0,
                -1, 1
        });
    }

    @Override
    public float getSpeed() {
        return 560;
    }
}
