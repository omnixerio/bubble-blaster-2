package dev.ultreon.bubbles.item;

import dev.ultreon.bubbles.ammo.AmmoType;

public class AmmoItem extends ItemType {
    private final AmmoType ammoType;

    public AmmoItem(AmmoType ammoType) {
        this.ammoType = ammoType;
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
}
