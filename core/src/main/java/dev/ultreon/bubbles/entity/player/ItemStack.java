package dev.ultreon.bubbles.entity.player;

import com.badlogic.gdx.graphics.Color;
import dev.ultreon.bubbles.item.ItemType;
import dev.ultreon.bubbles.render.Renderer;

public class ItemStack {
    private final ItemType item;
    private int count;

    public ItemStack(ItemType item) {
        this(item, 1);
    }

    public ItemStack(ItemType item, int count) {
        this.item = item;
        this.setCount(count);
    }

    public ItemType getType() {
        return this.item;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }

        this.count = Math.clamp(count, 1, this.item.getMaxStackSize());
    }

    public boolean grow(int count) {
        if (this.count + count >= this.item.getMaxStackSize()) {
            return false;
        }
        this.setCount(this.getCount() + count);
        return true;
    }

    public boolean shrink(int count) {
        if (this.count == 1) {
            return false;
        }
        this.setCount(this.getCount() - count);
        return true;
    }

    public void render(Renderer renderer, int x, int y, int width, int height, Color color) {
        this.getType().render(renderer, x, y, width, height, color);
    }
}
