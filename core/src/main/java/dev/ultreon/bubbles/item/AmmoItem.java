package dev.ultreon.bubbles.item;

import dev.ultreon.bubbles.ammo.AmmoType;
import dev.ultreon.bubbles.entity.Bullet;
import dev.ultreon.bubbles.entity.player.Hotbar;
import dev.ultreon.bubbles.entity.player.Player;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.bubbles.world.World;

import java.util.Objects;

public class AmmoItem extends ItemType {
    private final AmmoType ammoType;
    private int price;

    public AmmoItem(AmmoType ammoType, int price) {
        this.ammoType = ammoType;
        this.price = price;
    }

    @Override
    public int getMaxStackSize() {
        return 75;
    }

    @SuppressWarnings("EmptyMethod")
    public void inventoryTick() {

    }

    public AmmoType getAmmoType() {
        return this.ammoType;
    }

    @Override
    public int getPrice() {
        return this.price;
    }

    @Override
    public void use(Player player, Hotbar hotbar, int index) {
        super.use(player, hotbar, index);

        var rotation = player.getRotation();
        var world = player.getWorld();
        var bullet = new Bullet(this.ammoType, player, player.pos, rotation, world);
        bullet.setOwner(player);
        world.addEntity(bullet);
    }
}
