package dev.ultreon.bubbles.shop;

import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.ubo.types.DataType;
import dev.ultreon.ubo.types.MapType;

public interface ShopItem {
    int getPrice();

    void onBought();

    default boolean canBuy() {
        return true;
    }

    default MapType save(MapType map) {
        return map;
    }

    default void load(DataType<?> data) {

    }

    TextObject getName();

    String getId();
}
