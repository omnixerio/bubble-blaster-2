package dev.ultreon.bubbles.shop;

import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.item.ItemType;
import dev.ultreon.bubbles.item.ItemTypes;
import dev.ultreon.ubo.DataIo;
import dev.ultreon.ubo.types.DataType;
import dev.ultreon.ubo.types.MapType;

import java.io.IOException;
import java.util.*;

public class Shop {
    private static final Map<String, ItemType> ITEMS = new LinkedHashMap<>();
    private static final Map<String, Upgrade> UPGRADES = new LinkedHashMap<>();
    private static final Map<String, Boost> BOOSTS = new LinkedHashMap<>();
    private static final Map<String, Cosmetic> COSMETICS = new LinkedHashMap<>();

    public static void register() {
        // Register predefined upgrades
        Shop.addItem(ItemTypes.BASIC_AMMO);
        Shop.addItem(ItemTypes.ROCKET_AMMO);

        Shop.addItem(Upgrade.HEALTH);
        Shop.addItem(Upgrade.SPEED);
        Shop.addItem(Upgrade.AMMO_RELOAD_SPEED);
        Shop.addItem(Upgrade.AMMO_PIERCING);

        // Register predefined boosts
        Shop.addItem(Boost.SPEED_BOOST);
        Shop.addItem(Boost.ATTACK_BOOST);
        Shop.addItem(Boost.RESISTANCE_BOOST);

        // Register predefined cosmetics
        Shop.addItem(Cosmetic.GREEN_SKIN);
        Shop.addItem(Cosmetic.BLUE_SKIN);
        Shop.addItem(Cosmetic.FIRE_TRAIL);
    }

    public static void save() {
        var map = new MapType();
        var upgrades = new MapType();
        UPGRADES.forEach((id, upgrade) -> upgrades.put(id, upgrade.save(new MapType())));
        map.put("Upgrades", upgrades);
        var boosts = new MapType();
        BOOSTS.forEach((id, boost) -> boosts.put(id, boost.save(new MapType())));
        map.put("Boosts", boosts);
        var cosmetics = new MapType();
        COSMETICS.forEach((id, cosmetic) -> cosmetics.put(id, cosmetic.save(new MapType())));
        map.put("Cosmetics", cosmetics);
        try (var read = BubbleBlaster.data("shop.ubo").write(false)) {
            DataIo.write(map, read);
        } catch (IOException e) {
            BubbleBlaster.LOGGER.error("Failed to read shop data", e);
        }
    }

    public static void load() {
        var data = BubbleBlaster.data("shop.ubo");
        if (!data.exists()) return;
        if (data.isDirectory()) {
            data.deleteDirectory();
            return;
        }

        try (var read = data.read()) {
            MapType map = DataIo.read(read);
            var items = map.getMap("Items", new MapType());
            items.forEach((id, item) -> Shop.load(ITEMS, id, item));
            var upgrades = map.getMap("Upgrades", new MapType());
            upgrades.forEach((id, upgrade) -> Shop.load(UPGRADES, id, upgrade));
            var boosts = map.getMap("Boosts", new MapType());
            boosts.forEach((id, boost) -> Shop.load(BOOSTS, id, boost));
            var cosmetics = map.getMap("Cosmetics", new MapType());
            cosmetics.forEach((id, cosmetic) -> Shop.load(COSMETICS, id, cosmetic));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T extends ShopItem> void load(Map<String, T> upgrades, String id, DataType<?> upgrade) {
        var t = upgrades.get(id);

        if (t == null) {
            BubbleBlaster.LOGGER.warn("Could not find shop item with id {}", id);
            return;
        }

        t.load(upgrade);
    }

    public static void addItem(ShopItem item) {
        switch (item) {
            case ItemType itemType -> ITEMS.put(itemType.getId(), itemType);
            case Upgrade upgradeItem -> UPGRADES.put(upgradeItem.getId(), upgradeItem);
            case Boost boostItem -> BOOSTS.put(boostItem.getId(), boostItem);
            case Cosmetic cosmeticItem -> COSMETICS.put(cosmeticItem.getId(), cosmeticItem);
            case null, default -> throw new IllegalArgumentException("Invalid shop item type");
        }
    }

    public static List<ItemType> getItems() {
        return List.copyOf(ITEMS.values());
    }

    public static List<Upgrade> getUpgrades() {
        return List.copyOf(UPGRADES.values());
    }

    public static List<Boost> getBoosts() {
        return List.copyOf(BOOSTS.values());
    }

    public static List<Cosmetic> getCosmetics() {
        return List.copyOf(COSMETICS.values());
    }
}
