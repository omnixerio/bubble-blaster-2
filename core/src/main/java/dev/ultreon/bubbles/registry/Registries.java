package dev.ultreon.bubbles.registry;

import com.badlogic.gdx.graphics.Cursor;
import com.google.common.annotations.Beta;
import dev.ultreon.bubbles.audio.MusicEvent;
import dev.ultreon.bubbles.audio.SoundEvent;
import dev.ultreon.bubbles.bubble.BubbleType;
import dev.ultreon.bubbles.common.gamestate.GameplayEvent;
import dev.ultreon.bubbles.effect.StatusEffect;
import dev.ultreon.bubbles.ammo.AmmoType;
import dev.ultreon.bubbles.entity.flags.EntityFlag;
import dev.ultreon.bubbles.entity.player.ability.AbilityType;
import dev.ultreon.bubbles.entity.types.EntityType;
import dev.ultreon.bubbles.gamemode.Gamemode;
import dev.ultreon.bubbles.input.KeyBinding;
import dev.ultreon.bubbles.item.ItemType;
import dev.ultreon.bubbles.render.TextureCollection;
import dev.ultreon.bubbles.init.FontSet;
import dev.ultreon.bubbles.render.gui.hud.HudType;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.libs.registries.v0.Registry;



public class Registries {
    public static final Registry<AmmoType> AMMO_TYPES = Registry.create(new Identifier("ammo"));
    public static final Registry<EntityType<?>> ENTITIES = Registry.create(new Identifier("entity"));
    public static final Registry<BubbleType> BUBBLES = Registry.create(new Identifier("bubble"));
    public static final Registry<StatusEffect> EFFECTS = Registry.create(new Identifier("status_effect"));
    public static final Registry<AbilityType<?>> ABILITIES = Registry.create(new Identifier("ability"));
    public static final Registry<GameplayEvent> GAMEPLAY_EVENTS = Registry.create(new Identifier("gameplay_event"));
    public static final Registry<FontSet> BITMAP_FONTS = Registry.create(new Identifier("font"));
    public static final Registry<Gamemode> GAMEMODES = Registry.create(new Identifier("gamemode"));
    public static final Registry<Cursor> CURSORS = Registry.create(new Identifier("cursor"));
    public static final Registry<ItemType> ITEMS = Registry.create(new Identifier("item"));
    public static final Registry<TextureCollection> TEXTURE_COLLECTIONS = Registry.create(new Identifier("texture_collection"));
    public static final Registry<SoundEvent> SOUNDS = Registry.create(new Identifier("sound"));
    public static final Registry<MusicEvent> MUSIC = Registry.create(new Identifier("music"));
    public static final Registry<EntityFlag> ENTITY_FLAGS = Registry.create(new Identifier("entity_flag"));
    public static final Registry<HudType> HUD = Registry.create(new Identifier("hud"));
    public static final Registry<KeyBinding> KEY_BINDINGS = Registry.create(new Identifier("key_binding"));
}
