package dev.ultreon.bubbles.item;

import com.badlogic.gdx.graphics.Color;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.entity.player.Hotbar;
import dev.ultreon.bubbles.entity.player.ItemStack;
import dev.ultreon.bubbles.entity.player.Player;
import dev.ultreon.bubbles.init.Fonts;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.shop.ShopItem;
import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.libs.commons.v0.Identifier;

import java.util.Objects;

public abstract class ItemType implements IItemProvider, ShopItem {
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

    public void use(Player player, Hotbar hotbar, int index) {
        hotbar.shrink(index);
    }

    @Override
    public void onBought() {
        var game = BubbleBlaster.getInstance();
        game.hotbar.add(this);
    }

    @Override
    public boolean canBuy() {
        var game = BubbleBlaster.getInstance();
        return game.hotbar.canAdd(this);
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

    @Override
    public String getId() {
        return Objects.requireNonNull(Registries.ITEMS.getKey(this)).toString();
    }

    @Override
    public Identifier getIcon() {
        var itemId = Registries.ITEMS.getKey(this);
        if (itemId == null) return null;

        return new Identifier(itemId.location(), "items/" + itemId.path());
    }

    @Override
    public TextObject getName() {
        var id = Objects.requireNonNull(Registries.ITEMS.getKey(this));
        return TextObject.translation(id.location() + ".itemtype." + id.path().replace("/", "."));
    }
}
