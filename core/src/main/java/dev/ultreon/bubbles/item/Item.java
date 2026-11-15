package dev.ultreon.bubbles.item;

import dev.ultreon.bubbles.common.TagHolder;
import dev.ultreon.bubbles.common.interfaces.StateHolder;
import dev.ultreon.bubbles.entity.Entity;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.ubo.types.MapType;
import dev.ultreon.libs.commons.v0.Identifier;
import org.jetbrains.annotations.NotNull;

public final class Item implements IItemProvider, StateHolder, TagHolder {
    private ItemType type;
    private MapType tag;

    @Override
    public ItemType getItem() {
        return this.type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public ItemType getType() {
        return this.type;
    }

    @Override
    public @NotNull MapType save() {
        var data = new MapType();
        data.put("Tag", this.tag);
        data.putString("type", Registries.ITEMS.getKey(this.type).toString());

        return data;
    }

    @Override
    public void load(MapType tag) {
        this.tag = tag.getMap("Tag");
        this.type = Registries.ITEMS.getValue(Identifier.parse(tag.getString("type")));
    }

    @Override
    public MapType getTag() {
        return this.tag;
    }

    public void onEntityTick(Entity entity) {
        this.type.onEntityTick();
    }

    public void tick() {
        this.type.tick();
    }
}
