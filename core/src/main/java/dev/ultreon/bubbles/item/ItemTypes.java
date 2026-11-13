package dev.ultreon.bubbles.item;

import dev.ultreon.bubbles.init.AmmoTypes;

public class ItemTypes {
    public final AmmoItem BASIC_AMMO = new AmmoItem(AmmoTypes.BASIC);
    public final AmmoItem ROCKET_AMMO = new AmmoItem(AmmoTypes.ROCKET);

    public static void register() {

    }
}
