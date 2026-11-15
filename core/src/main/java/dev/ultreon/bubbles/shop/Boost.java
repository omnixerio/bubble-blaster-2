package dev.ultreon.bubbles.shop;

import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.ubo.types.DataType;
import dev.ultreon.ubo.types.MapType;

public class Boost implements ShopItem {
    public static final Boost SPEED_BOOST = new Boost("speed", 50, 5);
    public static final Boost ATTACK_BOOST = new Boost("attack", 50, 5);
    public static final Boost RESISTANCE_BOOST = new Boost("resistance", 50, 5);

    private final TextObject name;
    private final int price;
    private final int maxCount;
    private final String id;
    private int inventoryCount;

    public Boost(String id, int price, int maxCount) {
        this.id = "boosts/" + id;
        this.name = TextObject.translation("bubbleblaster.boosts." + id);
        this.price = price;
        this.maxCount = maxCount;
    }

    @Override
    public void load(DataType<?> data) {
        if (data instanceof MapType map) {
            this.inventoryCount = map.getInt("inventoryCount", this.inventoryCount);
        }
    }

    @Override
    public MapType save(MapType map) {
        map.putInt("inventoryCount", this.inventoryCount);
        return map;
    }

    @Override
    public int getPrice() {
        return this.price;
    }

    @Override
    public void onBought() {
        this.inventoryCount++;
    }

    @Override
    public boolean canBuy() {
        return this.inventoryCount < this.maxCount;
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
