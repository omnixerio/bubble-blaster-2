package dev.ultreon.bubbles.shop;

import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.ubo.types.DataType;
import dev.ultreon.ubo.types.MapType;

public class Upgrade implements ShopItem {
    public static final Upgrade HEALTH = new Upgrade("health", 100);
    public static final Upgrade SPEED = new Upgrade("speed", 100, 5);
    public static final Upgrade AMMO_RELOAD_SPEED = new Upgrade("ammo_reload_speed", 100, 5);
    public static final Upgrade AMMO_PIERCING = new Upgrade("ammo_piercing", 100, 10);

    private final TextObject name;
    private final int basePrice;
    private final int maxLevel;
    private int level = 0;
    private String id;

    public Upgrade(String name, int basePrice) {
        this(name, basePrice, -1);
    }

    public Upgrade(String id, int basePrice, int maxLevel) {
        this.id = "upgrades/" + id;
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
        return (int) (this.basePrice * ((this.level + 1) * 1.625));
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

    @Override
    public Identifier getIcon() {
        return new Identifier("bubbleblaster", this.id);
    }
}
