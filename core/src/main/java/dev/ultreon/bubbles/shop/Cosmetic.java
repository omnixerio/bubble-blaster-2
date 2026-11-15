package dev.ultreon.bubbles.shop;

import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.ubo.types.DataType;
import dev.ultreon.ubo.types.MapType;

public class Cosmetic implements ShopItem {
    public static final Cosmetic GREEN_SKIN = new Cosmetic("green_skin", 200);
    public static final Cosmetic BLUE_SKIN = new Cosmetic("blue_skin", 200);
    public static final Cosmetic FIRE_TRAIL = new Cosmetic("fire_trail", 300);

    private final TextObject name;
    private final String id;
    private final int price;
    private boolean bought;

    public Cosmetic(String id, int price) {
        this.name = TextObject.translation("bubbleblaster.cosmetics." + id);
        this.id = "cosmetics/" + id;
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

    @Override
    public Identifier getIcon() {
        return new Identifier("bubbleblaster", this.id);
    }
}
