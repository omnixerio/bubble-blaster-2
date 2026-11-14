package dev.ultreon.bubbles.entity.player;

import com.badlogic.gdx.graphics.Color;
import dev.ultreon.bubbles.init.Fonts;
import dev.ultreon.bubbles.render.Renderer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class Hotbar {
    private final ItemStack[] items = new ItemStack[5];
    private int selected;
    private float selectedF;

    public Hotbar() {
        this.selected = 0;
    }

    public ItemStack getSelectedItem() {
        return this.items[this.selected];
    }

    public int getSelected() {
        return this.selected;
    }

    public void setSelected(int selectedItem) {
        this.selected = Math.clamp(selectedItem, 0, this.items.length - 1);
    }

    @Nullable
    public ItemStack get(int selected) {
        return this.items[Math.clamp(selected, 0, this.items.length - 1)];
    }

    public void set(int selected, @Nullable ItemStack item) {
        this.items[Math.clamp(selected, 0, this.items.length - 1)] = item;
    }

    public void render(Renderer renderer) {
        this.item(renderer, renderer.getWidth() / 2 - 20 - 40 - 40, 22, 36, 36, Color.WHITE, 0);
        this.item(renderer, renderer.getWidth() / 2 - 20 - 40, 22, 36, 36, Color.WHITE, 1);
        this.item(renderer, renderer.getWidth() / 2 - 20, 22, 36, 36, Color.WHITE, 2);
        this.item(renderer, renderer.getWidth() / 2 + 20, 22, 36, 36, Color.WHITE, 3);
        this.item(renderer, renderer.getWidth() / 2 + 20 + 40, 22, 36, 36, Color.WHITE, 4);

        var x = (renderer.getWidth() / 2 - 20 - 40 - 40 + 2) + selected * 40;
        renderer.fillEffect(x, 58, 36, 2);
    }

    private void item(Renderer renderer, int x, int y, int width, int height, Color color, int slot) {
        var item = this.items[slot];
        if (item != null) {
            item.render(renderer, x, y, width, height, color);
            renderer.drawTextRight(Fonts.DEFAULT.get(), String.valueOf(slot), x + width - 4, y + height - 4, Color.WHITE);
        }
    }

    public void mouseWheel(int x, int y, float amountY) {
        this.selectedF = Math.clamp(this.selectedF + amountY, 0, this.items.length - 1);
        this.selected = (int) this.selectedF;
    }
}
