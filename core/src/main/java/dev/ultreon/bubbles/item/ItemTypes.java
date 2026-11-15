package dev.ultreon.bubbles.item;

import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.init.AmmoTypes;
import dev.ultreon.bubbles.registry.Registries;

public class ItemTypes {
    public static final AmmoItem BASIC_AMMO = register("basic_ammo", new AmmoItem(AmmoTypes.BASIC, 100));
    public static final AmmoItem ROCKET_AMMO = register("rocket_ammo", new AmmoItem(AmmoTypes.ROCKET, 3000));

    private static <T extends ItemType> T register(String name, T itemType) {
        Registries.ITEMS.register(BubbleBlaster.id(name), itemType);
        return itemType;
    }

    public static void register() {

    }
}
