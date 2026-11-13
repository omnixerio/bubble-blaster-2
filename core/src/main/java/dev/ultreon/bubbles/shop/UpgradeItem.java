package dev.ultreon.bubbles.shop;

import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.ubo.types.DataType;
import dev.ultreon.ubo.types.MapType;

public class UpgradeItem implements ShopItem {
    public static final UpgradeItem HEALTH = new UpgradeItem("health", 100);
    public static final UpgradeItem SPEED = new UpgradeItem("speed", 100, 5);
    public static final UpgradeItem AMMO_RELOAD_SPEED = new UpgradeItem("ammo_reload_speed", 100, 5);
    public static final UpgradeItem AMMO_PIERCING = new UpgradeItem("ammo_piercing", 100, 10);

    private final TextObject name;
    private final int basePrice;
    private final int maxLevel;
    private int level = 0;
    private String id;

    public UpgradeItem(String name, int basePrice) {
        this(name, basePrice, -1);
    }

    public UpgradeItem(String id, int basePrice, int maxLevel) {
        this.id = "upgrade/" + id;
        this.name = TextObject.translation("bubbleblaster.upgrades." + id);
        this.basePrice = basePrice;
        this.maxLevel = maxLevel;
    }

    public int getLevel() {
        return this.level;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public boolean isMaxed() {
        if (this.maxLevel == -1)
            return false;

        return this.level >= this.maxLevel;
    }

    public boolean hasFiniteLevels() {
        return this.maxLevel != -1;
    }

    @Override
    public int getPrice() {
        return (int) (this.basePrice * (this.level * 1.625));
    }

    @Override
    public void onBought() {
        this.level++;
    }

    @Override
    public MapType save(MapType map) {
        map.putInt("level", this.level);
        return map;
    }

    @Override
    public void load(DataType<?> data) {
        if (data instanceof MapType map)
            this.level = map.getInt("level", this.level);
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
