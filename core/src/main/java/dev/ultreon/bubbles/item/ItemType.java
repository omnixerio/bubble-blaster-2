package dev.ultreon.bubbles.item;

import com.badlogic.gdx.graphics.Color;
import dev.ultreon.bubbles.init.Fonts;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.bubbles.render.Renderer;

public abstract class ItemType implements IItemProvider {
    @Override
    public ItemType getItem() {
        return this;
    }

    public int getMaxStackSize() {
        return 5;
    }

    @SuppressWarnings("EmptyMethod")
    public void onEntityTick() {

    }

    @SuppressWarnings("EmptyMethod")
    public void tick() {

    }

    public void render(Renderer renderer, int x, int y, int width, int height, Color color) {
        var key = Registries.ITEMS.getKey(this);
        if (key == null) {
            renderer.line(x, y, x + width, y + height, Color.WHITE);
            renderer.line(x, y + width, x + width, y, Color.WHITE);
            return;
        }
        var path = key.path();
        renderer.drawTextCenter(Fonts.DEFAULT.get(), path, x + (float) width / 2, y + (float) height / 2, Color.WHITE);
    }
}
