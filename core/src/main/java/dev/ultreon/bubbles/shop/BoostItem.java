package dev.ultreon.bubbles.shop;

import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.ubo.types.DataType;
import dev.ultreon.ubo.types.MapType;

public class BoostItem implements ShopItem {
    public static final BoostItem SPEED_BOOST = new BoostItem("speed", 50, 5);
    public static final BoostItem ATTACK_BOOST = new BoostItem("attack", 50, 5);
    public static final BoostItem RESISTANCE_BOOST = new BoostItem("resistance", 50, 5);

    private final TextObject name;
    private final int price;
    private final int maxCount;
    private final String id;
    private int inventoryCount;

    public BoostItem(String id, int price, int maxCount) {
        this.id = "boost/" + id;
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
}
