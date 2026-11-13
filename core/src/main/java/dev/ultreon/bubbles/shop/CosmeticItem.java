package dev.ultreon.bubbles.shop;

import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.ubo.types.DataType;
import dev.ultreon.ubo.types.MapType;

public class CosmeticItem implements ShopItem {
    public static final CosmeticItem GREEN_SKIN = new CosmeticItem("green_skin", 200);
    public static final CosmeticItem BLUE_SKIN = new CosmeticItem("blue_skin", 200);
    public static final CosmeticItem FIRE_TRAIL = new CosmeticItem("fire_trail", 300);

    private final TextObject name;
    private final String id;
    private final int price;
    private boolean bought;

    public CosmeticItem(String id, int price) {
        this.name = TextObject.translation("bubbleblaster.cosmetics." + id);
        this.id = "cosmetic/" + id;
        this.price = price;
    }

    @Override
    public void load(DataType<?> data) {
        if (data instanceof MapType map) {
            this.bought = map.getBoolean("bought", this.bought);
        }
    }

    @Override
    public MapType save(MapType map) {
        map.putBoolean("bought", this.bought);
        return ShopItem.super.save(map);
    }

    @Override
    public int getPrice() {
        return this.price;
    }

    @Override
    public void onBought() {
        this.bought = true;
    }

    @Override
    public boolean canBuy() {
        return !this.bought;
    }

    @Override
    public TextObject getName() {
        return this.name;
    }

    @Override
    public String getId() {
        return this.id;
    }
}
