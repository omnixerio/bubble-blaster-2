package dev.ultreon.bubbles.entity.player;

import com.badlogic.gdx.graphics.Color;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.init.Fonts;
import dev.ultreon.bubbles.item.ItemType;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.ubo.DataIo;
import dev.ultreon.ubo.types.ListType;
import dev.ultreon.ubo.types.MapType;
import org.jetbrains.annotations.Nullable;

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
        this.save();
    }

    public void render(Renderer renderer) {
        this.item(renderer, renderer.getWidth() / 2 - 20 - 40 - 40, 22, 36, 36, Color.WHITE, 0);
        this.item(renderer, renderer.getWidth() / 2 - 20 - 40, 22, 36, 36, Color.WHITE, 1);
        this.item(renderer, renderer.getWidth() / 2 - 20, 22, 36, 36, Color.WHITE, 2);
        this.item(renderer, renderer.getWidth() / 2 + 20, 22, 36, 36, Color.WHITE, 3);
        this.item(renderer, renderer.getWidth() / 2 + 20 + 40, 22, 36, 36, Color.WHITE, 4);

        var x = (renderer.getWidth() / 2 - 20 - 40 - 40 + 2) + this.selected * 40;
        renderer.fillEffect(x, 58, 36, 2);
    }

    private void item(Renderer renderer, int x, int y, int width, int height, Color color, int slot) {
        var item = this.items[slot];
        if (item != null) {
            var icon = item.getType().getIcon();
            if (icon != null) {
                var texture = BubbleBlaster.getInstance().getTextureManager().getOrLoadTexture(icon);
                renderer.blit(texture, x, y, width, height);
            } else {
                item.render(renderer, x, y, width, height, color);
            }
            renderer.drawTextRight(Fonts.DEFAULT.get(), String.valueOf(slot), x + width - 4, y + height - 4, Color.WHITE);
        }
    }

    public void mouseWheel(int x, int y, float amountY) {
        this.selectedF = Math.clamp(this.selectedF + amountY, 0, this.items.length - 1);
        this.selected = (int) this.selectedF;
    }

    public void load() {
        var child = BubbleBlaster.getDataDir().child("hotbar.ubo");
        if (!child.exists()) return;

        try (var input = child.read()) {
            var list = DataIo.<ListType<MapType>>read(input);
            for (var i = 0; i < this.items.length; i++) {
                var mapType = list.get(i);
                if (mapType.getBoolean("empty")) {
                    this.items[i] = null;
                } else {
                    this.items[i] = ItemStack.load(mapType);
                }
            }
        } catch (Exception e) {
            BubbleBlaster.crash(e);
        }

        this.save();
    }

    public void save() {
        var data = new ListType<MapType>();
        for (var i = 0; i < this.items.length; i++) {
            var item = this.items[i];
            MapType itemData;
            if (item != null) {
                itemData = item.save();
                itemData.putBoolean("empty", false);
            } else {
                itemData = new MapType();
                itemData.putBoolean("empty", true);
            }
            data.add(i, itemData);
        }

        try (var output = BubbleBlaster.getDataDir().child("hotbar.ubo").write(false)) {
            DataIo.write(data, output);
        } catch (Exception e) {
            BubbleBlaster.crash(e);
        }
    }

    public boolean add(ItemType itemType) {
        for (var i = 0; i < this.items.length; i++) {
            if (this.items[i] == null) {
                this.items[i] = new ItemStack(itemType);
                this.save();
                return true;
            } else if (this.items[i].getType() == itemType && this.items[i].maxStackSize() < this.items[i].getCount()) {
                this.items[i].grow(1);
                this.save();
                return true;
            }
        }
        return false;
    }
    public boolean canAdd(ItemType itemType) {
        for (var item : this.items) {
            if (item == null) {
                return true;
            } else if (item.getType() == itemType && item.maxStackSize() < item.getCount()) {
                return true;
            }
        }
        return false;
    }

    public void shrink(int index) {
        var item = this.items[index];
        if (item == null) return;

        if (!item.shrink(1)) this.items[index] = null;
        this.save();
    }
}
