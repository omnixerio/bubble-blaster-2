package dev.ultreon.bubbles.shop;

import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.entity.Bubble;
import dev.ultreon.ubo.DataIo;
import dev.ultreon.ubo.types.DataType;
import dev.ultreon.ubo.types.MapType;

import java.io.IOException;
import java.util.*;

public class Shop {
    private static final Map<String, UpgradeItem> UPGRADES = new LinkedHashMap<>();
    private static final Map<String, BoostItem> BOOSTS = new LinkedHashMap<>();
    private static final Map<String, CosmeticItem> COSMETICS = new LinkedHashMap<>();

    public static void register() {
        // Register predefined upgrades
        Shop.addItem(UpgradeItem.HEALTH);
        Shop.addItem(UpgradeItem.SPEED);
        Shop.addItem(UpgradeItem.AMMO_RELOAD_SPEED);
        Shop.addItem(UpgradeItem.AMMO_PIERCING);

        // Register predefined boosts
        Shop.addItem(BoostItem.SPEED_BOOST);
        Shop.addItem(BoostItem.ATTACK_BOOST);
        Shop.addItem(BoostItem.RESISTANCE_BOOST);

        // Register predefined cosmetics
        Shop.addItem(CosmeticItem.GREEN_SKIN);
        Shop.addItem(CosmeticItem.BLUE_SKIN);
        Shop.addItem(CosmeticItem.FIRE_TRAIL);
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
            var upgrades = map.getMap("Upgrades");
            UPGRADES.clear();
            upgrades.forEach((id, upgrade) -> Shop.load(UPGRADES, id, upgrade));
            var boosts = map.getMap("Boosts");
            BOOSTS.clear();
            boosts.forEach((id, boost) -> Shop.load(BOOSTS, id, boost));
            var cosmetics = map.getMap("Cosmetics");
            COSMETICS.clear();
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
            case UpgradeItem upgradeItem -> UPGRADES.put(upgradeItem.getId(), upgradeItem);
            case BoostItem boostItem -> BOOSTS.put(boostItem.getId(), boostItem);
            case CosmeticItem cosmeticItem -> COSMETICS.put(cosmeticItem.getId(), cosmeticItem);
            case null, default -> throw new IllegalArgumentException("Invalid shop item type");
        }
    }

    public static List<UpgradeItem> getUpgrades() {
        return List.copyOf(UPGRADES.values());
    }

    public static List<BoostItem> getBoosts() {
        return List.copyOf(BOOSTS.values());
    }

    public static List<CosmeticItem> getCosmetics() {
        return List.copyOf(COSMETICS.values());
    }
}
