package dev.ultreon.bubbles.gamemode;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.bubble.BubbleType;
import dev.ultreon.bubbles.common.Controllable;
import dev.ultreon.bubbles.common.random.BubbleRandomizer;
import dev.ultreon.bubbles.entity.Entity;
import dev.ultreon.bubbles.entity.bubble.BubbleSystem;
import dev.ultreon.bubbles.entity.player.Player;
import dev.ultreon.bubbles.entity.spawning.SpawnInformation;
import dev.ultreon.bubbles.entity.spawning.SpawnUsage;
import dev.ultreon.bubbles.init.BubbleTypes;
import dev.ultreon.bubbles.random.RandomSource;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.hud.HudType;
import dev.ultreon.bubbles.render.gui.screen.Screen;
import dev.ultreon.bubbles.save.GameSave;
import dev.ultreon.bubbles.vector.Vector2D;
import dev.ultreon.bubbles.world.World;
import dev.ultreon.bubbles.util.annotation.MethodsReturnNonnullByDefault;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.libs.commons.v0.Messenger;
import dev.ultreon.bubbles.text.TextObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author XyperCode
 * @see HudType
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings({"UnusedReturnValue", "BooleanMethodIsAlwaysInverted", "RedundantThrows", "UnnecessaryLocalVariable"})
public abstract class Gamemode implements Controllable {
    // Types.
    protected final BubbleBlaster game = BubbleBlaster.getInstance();

    // Scene
    protected Screen screen;

    protected boolean initialized = false;
    private BubbleType defaultBubble = BubbleTypes.NORMAL;
    private boolean active;
    private final int randomIdx = 0;
    private HudType hud;

    public long getSeed(World world) {
        return world.getSeed();
    }

    public Gamemode() {

    }

    /**
     * This method gets called when the {@linkplain World world} is loaded without any previously saved data.
     *
     * @param world the world that's getting loaded.
     * @param messenger   messaging system for showing loading information when the save is being prepared for first init.
     */
    public void onFirstInit(World world, Messenger messenger) {

    }

    /**
     * Handles loading create world.
     * Made for loading things into other classes that are aware create world load / unload.
     *
     * @param world the world that's loading.
     * @param save        game save to load from.
     * @param messenger   messaging system for showing loading information when a save is getting loaded.
     */
    public void onLoad(World world, GameSave save, Messenger messenger) {

    }

    /**
     * Load State from Bytearray.
     * Loads the game-type state from a bytearray.
     *
     * @param save      a bytearray create data to get the game-type from.
     * @param Messenger info transporter for showing current status to load scene or save loading scene.
     * @return the game-type loaded from the save.
     */
    public static Gamemode loadState(GameSave save, Messenger Messenger) throws IOException {
        return save.getInfo().getGamemode();
    }

    /**
     * Get game-type build version.
     *
     * @return the game-type version.
     */
    public int getGameTypeVersion() {
        return -1;
    }

    /**
     * Check for missing entries in the registry to load the saved game.
     *
     * @param gameSave the saved game to check for.
     * @return a hashmap container as key the registry, and as value a list create missing resource locations create the registry.
     * @throws IOException when an I/O error occurred.
     */
    @Deprecated
    public HashMap<Registries, List<Identifier>> checkRegistry(GameSave gameSave) throws IOException {
        var missing = new HashMap<Registries, List<Identifier>>();

        return missing;
    }

    @Override
    public void begin() {
        var hudOverride = this.getHudOverride();
        this.hud = hudOverride != null ? hudOverride : HudType.getCurrent();
        this.hud.begin();

        this.active = true;
    }

    @Nullable
    public HudType getHudOverride() {
        return null;
    }

    @Override
    public void end() {
        if (this.hud != null) {
            this.hud.end();
            this.hud = null;
        }
        this.active = false;
    }

    public boolean isActive() {
        return this.active;
    }

    public abstract Rectangle getGameBounds();

    @Nullable
    public abstract Player getPlayer();

    public abstract void onGameOver();

    /**
     * Used for modifying the spawn location of an entity.
     *
     * @param entity the entity to get the spawn location for.
     * @param pos    the position passed to the entity.
     * @param usage  the spawn usage.
     * @param random the source of random to maybe determine the location.
     * @param retry  the amount of retries it took to spawn the entity.
     * @return the spawn location for that entity, or null to use the entity location given to the entity using {@link SpawnInformation}.
     * @see World#spawn(Entity, SpawnInformation)
     */
    public @Nullable Vector2D getSpawnPos(@NotNull Entity entity, @Nullable Vector2D pos, @NotNull SpawnUsage usage, @NotNull RandomSource random,  int retry) {
        return null;
    }

    public boolean doesSpawn(Entity entity) {
        return true;
    }

    @Deprecated(forRemoval = true)
    public void onQuit() {
        this.end();
    }

    public boolean renderBackground(Renderer renderer, BubbleBlaster game) {
        return false;
    }

    public void drawBubble(Renderer renderer, double x, double y, int radius, Color... colors) {
        double i = 0f;
        for (var color : colors) {
            if (i == 0) {
                if (colors.length >= 2) {
                    renderer.setLineThickness(2.2f);
                } else {
                    renderer.setLineThickness(2.0f);
                }
            } else if (i == colors.length - 1) {
                renderer.setLineThickness(2.0f);
            } else {
                renderer.setLineThickness(2.2f);
            }

            var ellipse = this.getCircle(x - (float) radius / 2, y - (float) radius / 2, radius, i);
            renderer.outline(ellipse, color);

            i += 2f;
        }
    }

    private Circle getCircle(double x, double y, double r, double i) {
        return new Circle((float) (x + i), (float) (y + i), (float) (r - i * 2f));
    }

    public void tick(World world) {

    }

    public abstract long getEntityId(Entity entity, World world, long spawnIndex, int retry);

    public BubbleRandomizer createBubbleRandomizer() {
        return new BubbleRandomizer();
    }

    public final BubbleType getDefaultBubble() {
        return this.defaultBubble;
    }

    protected final void setDefaultBubble(BubbleType defaultBubble) {
        this.defaultBubble = defaultBubble;
    }

    public void onSave(World world, GameSave save, Messenger messenger) {

    }

    public void onLevelUp(Player player, int to) {

    }

    public TextObject getName() {
        return TextObject.translation(this.getTranslationId());
    }

    public String getTranslationId() {
        var id = this.getId();
        return id.location() + ".gamemode." + id.path();
    }

    public Identifier getId() {
        return Objects.requireNonNull(Registries.GAMEMODES.getKey(this), "Gamemode not registered: " + this.getClass().getName());
    }

    /**
     * @return true if post-spawn was changed.
     */
    public boolean onPostSpawn() {
        return false;
    }

    /**
     *
     * @return true to cancel default spawn
     */
    public boolean preSpawn() {
        return false;
    }

    /**
     * Randomly selects a bubble. Or just gives a constant output.
     *
     * @param random the source of random to select a random bubble type.
     * @param world the world where it's going to be used.
     * @return The randomly selected bubble type, or null to use default behaviour in {@link World#getRandomBubble(RandomSource)}
     * @see BubbleSystem#random(RandomSource, World)
     */
    @Nullable
    public BubbleType randomBubble(RandomSource random, World world) {
        return null;
    }

    public boolean firstInit(Messenger messenger, int maxBubbles) {
        return false;
    }

    public int getBulletPops() {
        return 1;
    }

    public boolean canBePaused() {
        return true;
    }

    public void teleport(Entity entity, Vector2D old, Vector2D dest) {
        entity.pos.set(dest);
    }
}
