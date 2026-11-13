package dev.ultreon.bubbles.world;

import com.badlogic.gdx.math.Vector2;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.ultreon.bubbles.*;
import dev.ultreon.bubbles.audio.MusicEvent;
import dev.ultreon.bubbles.bubble.BubbleSpawnContext;
import dev.ultreon.bubbles.bubble.BubbleType;
import dev.ultreon.bubbles.common.Difficulty;
import dev.ultreon.bubbles.common.gamestate.GameplayContext;
import dev.ultreon.bubbles.common.gamestate.GameplayEvent;
import dev.ultreon.bubbles.common.random.BubbleRandomizer;
import dev.ultreon.bubbles.entity.Bubble;
import dev.ultreon.bubbles.entity.Entity;
import dev.ultreon.bubbles.entity.LivingEntity;
import dev.ultreon.bubbles.entity.bubble.BubbleSystem;
import dev.ultreon.bubbles.entity.damage.EntityDamageSource;
import dev.ultreon.bubbles.entity.player.Player;
import dev.ultreon.bubbles.entity.spawning.NaturalSpawnReason;
import dev.ultreon.bubbles.entity.spawning.SpawnInformation;
import dev.ultreon.bubbles.entity.spawning.SpawnUsage;
import dev.ultreon.bubbles.entity.types.EntityType;
import dev.ultreon.bubbles.event.v1.EntityEvents;
import dev.ultreon.bubbles.event.v1.EntityEvents.Spawn;
import dev.ultreon.bubbles.event.v1.PlayerEvents;
import dev.ultreon.bubbles.event.v1.TickEvents;
import dev.ultreon.bubbles.event.v1.WorldEvents;
import dev.ultreon.bubbles.gamemode.Gamemode;
import dev.ultreon.bubbles.gamemode.openworld.OpenWorldMode;
import dev.ultreon.bubbles.gameplay.GameplayStorage;
import dev.ultreon.bubbles.gameplay.event.BloodMoonGameplayEvent;
import dev.ultreon.bubbles.init.Gamemodes;
import dev.ultreon.bubbles.init.GameplayEvents;
import dev.ultreon.bubbles.random.JavaRandom;
import dev.ultreon.bubbles.random.RandomSource;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.bubbles.render.gui.hud.HudType;
import dev.ultreon.bubbles.render.gui.screen.GameOverScreen;
import dev.ultreon.bubbles.save.GameSave;
import dev.ultreon.bubbles.util.Comparison;
import dev.ultreon.bubbles.util.Randomizer;
import dev.ultreon.bubbles.vector.Vector2D;
import dev.ultreon.libs.commons.v0.DummyMessenger;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.libs.commons.v0.Messenger;
import dev.ultreon.libs.crash.v0.CrashCategory;
import dev.ultreon.libs.crash.v0.CrashLog;
import dev.ultreon.libs.registries.v0.Registry;
import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.ubo.types.ListType;
import dev.ultreon.ubo.types.LongType;
import dev.ultreon.ubo.types.MapType;
import dev.ultreon.ubo.types.StringType;
import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMaps;
import org.jetbrains.annotations.ApiStatus;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static dev.ultreon.bubbles.BubbleBlaster.TPS;
import static dev.ultreon.bubbles.BubbleBlaster.isOnTickingThread;

@SuppressWarnings("NewApi")
public final class World implements CrashFiller, Closeable {
    private static final long MINIMUM_ENTITY_ID = 0;
    private final List<Player> players = new CopyOnWriteArrayList<>();
    private Gamemode gamemode;
    private final long seed;
    private GameplayEvent activeEvent;

    // Flags.
    private volatile boolean gameOver = false;
    private boolean bubblesFrozen = false;

    // Enums.
    private Difficulty difficulty;

    // State difficulties.
    private final Map<GameplayEvent, Float> stateDifficultyModifiers = new ConcurrentHashMap<>();

    // Animations:

    // Modifiers
    private float globalBubbleSpeedModifier = 1;
    private float stateDifficultyModifier = 1;
    private final HashSet<GameplayEvent> gameplayEventActive = new HashSet<>();
    private final GameSave gameSave;

    // Values:
    private long resultScore;
    
    private long ticks;

    private final Difficulty.ModifierMap difficultyModifiers = new Difficulty.ModifierMap();

    private final BubbleRandomizer bubbleRng;
    private boolean initialized;

    // Locks
    private final ReentrantLock gameOverLock = new ReentrantLock();
    private final ReentrantLock entitiesLock = new ReentrantLock();
    private final Long2ReferenceMap<Entity> entitiesById = Long2ReferenceMaps.synchronize(new Long2ReferenceArrayMap<>());
    private final Map<UUID, Entity> entitiesByUuid = new ConcurrentHashMap<>();

    // Game
    private final BubbleBlaster game = BubbleBlaster.getInstance();
    private String name = "UNKNOWN WORLD";
    private int bubblesFrozenTicks;
    boolean shuttingDown;
    private GameplayStorage gameplayStorage = new GameplayStorage();
    public final ReentrantLock saveLock = new ReentrantLock(true);
    private long entitySeedIdx;
    private Instant gameOverTime = null;
    private long nextEntityId = 0;
    private final MusicEvent music = null;
    private final RandomSource randomSource;
    private int nextBloodMoon;
    private int maxBubbles;

    /**
     * Create a new world instance.
     *
     * @param save       the game save.
     * @param gamemode   the gamemode
     * @param difficulty the difficulty
     * @param seed       the seed
     * @see #World(GameSave, Gamemode, Difficulty, long)
     */
    public World(GameSave save, Gamemode gamemode, Difficulty difficulty, int seed) {
        this(save, gamemode, difficulty, (long) seed);
    }

    /**
     * Create a new world instance.
     * Note: This also resets the {@link BubbleSystem}.
     *
     * @param save       the game save.
     * @param gamemode   the gamemode
     * @param difficulty the difficulty
     * @param seed       the seed
     */
    public World(GameSave save, Gamemode gamemode, Difficulty difficulty, long seed) {
        this.gamemode = gamemode;
        this.difficulty = difficulty;

        BubbleSystem.reset();
        this.bubbleRng = this.gamemode.createBubbleRandomizer();
        this.seed = seed;
        this.gameSave = save;
        this.randomSource = new JavaRandom(seed ^ 0x58fa2bd933ec8ed3L);

        this.maxBubbles = 500;

        this.updateNextBloodMoon();
    }

    public void firstInit(Messenger messenger) {
        WorldEvents.WORLD_STARTING.factory().onWorldStarting(this);

        try {
            // Spawn player
            messenger.send("Spawning player...");
            var pos = new Vector2D(this.game.getScaledWidth() / 4f, BubbleBlaster.getInstance().getHeight() / 2f);
            var player = this.game.loadPlayerIntoWorld(this);
            this.spawn(player, SpawnInformation.playerSpawn(pos, this, new JavaRandom()));

            if (!this.gamemode.preSpawn()) {
                // Spawn bubbles
                messenger.send("Spawning bubbles...");

                this.firstInit(messenger, this.maxBubbles);
            }

            this.gamemode.onPostSpawn();
        } catch (Exception e) {
            var crashLog = new CrashLog("Could not initialize world.", e);

            BubbleBlaster.crash(crashLog.createCrash());
            return;
        }

        this.gamemode.onFirstInit(this, messenger);
        this.initialized = true;

        WorldEvents.WORLD_STARTED.factory().onWorldStarted(this);
    }

    public void updateNextBloodMoon() {
        this.nextBloodMoon = this.randomSource.nextInt(BubbleBlasterConfig.BLOOD_MOON_TRIGGER_LOW.get(), BubbleBlasterConfig.BLOOD_MOON_TRIGGER_HIGH.get()) * TPS;
    }

    private void firstInit(Messenger messenger, int maxBubbles) {
        if (this.gamemode.firstInit(messenger, maxBubbles)) return;

        for (var i = 0; i < maxBubbles; i++) {
            var retry = 0;

            var idx = this.entitySeedIdx++;
            var random = new JavaRandom(this.getSeed() ^ idx).nextRandom(Randomizer.hash(retry));
            var bubble = new Bubble(this, Bubble.getRandomVariant(this, random));
            this.spawn(bubble, SpawnInformation.naturalSpawn(null, random, SpawnUsage.BUBBLE_INIT_SPAWN, retry, this));

            messenger.send("Spawning bubble " + i + "/" + maxBubbles);
        }
    }

    public void load(GameSave save, Messenger messenger) throws IOException {
        WorldEvents.WORLD_STARTING.factory().onWorldStarting(this);

        this.name = save.getInfo().getName();

        this.loadWorld(save.load("world", true));
        this.gamemode.onLoad(this, save, messenger);

        this.gameplayStorage = new GameplayStorage(save.load("gameplay"));

        this.initialized = true;

        WorldEvents.WORLD_STARTED.factory().onWorldStarted(this);
    }

    public void save() {
        try {
            this.save(this.gameSave, new DummyMessenger());
        } catch (IOException e) {
            BubbleBlaster.getLogger().error("Error occurred when saving the game: ", e);
        }
    }

    public boolean save(GameSave save, Messenger messenger) throws IOException {
        if (!this.saveLock.tryLock()) return false;
        if (WorldEvents.WORLD_SAVING.factory().onWorldSaving(this, save, messenger).isCanceled()) return false;

        // Gamemode implementation for saving data.
        this.gamemode.onSave(this, save, messenger);

        // Save world data.
        this.dumpRegistries(save, messenger);
        this.dumpPlayers(save, messenger);
        save.dump("world", this.saveWorld(), true);
        save.dump("info", this.saveInfo(), true);
        save.dump("gameplay", this.gameplayStorage.save());

        this.saveLock.unlock();
        WorldEvents.WORLD_SAVED.factory().onWorldSaved(this, save, messenger);
        return true;
    }

    private void dumpPlayers(GameSave save, Messenger messenger) throws IOException {
        messenger.send("Saving players...");
        for (var p : this.players) {
            this.dumpPlayer(save, p);
        }
    }

    private void dumpPlayer(GameSave save, Player player) throws IOException {
        var data = player.save();
        var uniqueId = player.getUuid();
        save.dump("players/" + uniqueId, data, true);
    }

    private void loadPlayer(GameSave save, UUID uuid) throws IOException {
        var data = save.load("Players/" + uuid, true);
        var player = new Player(this);
        player.load(data);
        this.players.add(player);

        this.addEntity(player);
    }

    public void addEntity(Entity entity) {
        Preconditions.checkNotNull(entity, "Entity should not be null");
        this.entitiesLock.lock();
        this.addEntityUnlocked(entity);
        this.entitiesLock.unlock();
    }

    private void addEntityUnlocked(Entity entity) {
        if (entity.getId() < MINIMUM_ENTITY_ID)
            entity.setId(this.nextId());

        var uuid = UUID.randomUUID();
        while (this.entitiesByUuid.containsKey(uuid))
            uuid = UUID.randomUUID();

        entity.setUuid(uuid);

        this.entitiesById.put(entity.getId(), entity);
        this.entitiesByUuid.put(entity.getUuid(), entity);
    }

    private void dumpRegistries(GameSave save, Messenger messenger) throws IOException {
        messenger.send("Dumping registries.");
        for (var registry : Registry.getRegistries()) {
            this.dumpRegistryData(save, registry);
        }
    }

    private <T> void dumpRegistryData(GameSave gameSave, Registry<T> registry) throws IOException {
        var tag = new MapType();
        var entriesTag = new ListType<StringType>();

        for (var type : registry.values()) {
            entriesTag.add(new StringType(Objects.requireNonNull(registry.getKey(type)).toString()));
        }
        tag.put("Entries", entriesTag);

        gameSave.createFolders("registries/" + registry.id().location() + "/");
        gameSave.dump("registries/" + registry.id().location() + "/" + registry.id().path().replaceAll("/", "-"), tag);
    }

    private void loadWorld(MapType data) {
        ListType<MapType> entitiesTag = data.getList("Entities");
        for (var entityTag : entitiesTag) {
            var entity = Entity.loadFully(this, entityTag);
            if (entity != null) this.addEntity(entity);
        }

        this.game.player = (Player) Entity.loadFully(this, data.getMap("Player"));
        this.gameOver = data.getBoolean("gameOver", false);
        if (data.<LongType>contains("gameOverTime")) {
            this.gameOverTime = Instant.ofEpochSecond(data.getLong("gameOverTime"));
        }
        this.gamemode = Registries.GAMEMODES.getValue(Identifier.parse(data.getString("gamemode")));
        this.entitySeedIdx = data.getLong("entitySeedIdx");
        this.nextEntityId = data.getLong("nextEntityId");
    }

    private MapType saveWorld() {
        var data = new MapType();
        var entitiesData = new ListType<MapType>();
        for (var entity : this.entitiesById.values()) {
            entitiesData.add(entity.save());
        }
        data.put("Entities", entitiesData);
        data.put("Player", this.game.player.save());
        var uuid = this.game.player.getUuid();
        if (uuid != null) {
            data.putUUID("playerUuid", uuid);
        }
        data.putBoolean("gameOver", this.gameOver);
        if (this.gameOverTime != null) data.putLong("gameOverTime", this.gameOverTime.toEpochMilli());
        data.putLong("entitySeedIdx", this.entitySeedIdx);
        data.putLong("nextEntityId", this.nextEntityId);
        return data;
    }

    private MapType saveInfo() {
        var tag = new MapType();
        tag.putString("name", this.name);
        tag.putLong("seed", this.seed);
        tag.putLong("savedTime", LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC));
        var key = Registries.GAMEMODES.getKey(this.gamemode);
        tag.putString("gamemode", (key == null ? Gamemodes.NORMAL.id() : key).toString());
        return tag;
    }

    public BubbleRandomizer getBubbleRandomizer() {
        return this.bubbleRng;
    }

    @CanIgnoreReturnValue
    public boolean triggerGameOver() {
        return this.triggerGameOver(GameOverScreen.TITLE);
    }

    @CanIgnoreReturnValue
    public boolean triggerGameOver(TextObject title) {
        if (!BubbleBlaster.isOnTickingThread())
            throw new IllegalCallerException("Called on wrong thread! Should be on ticking thread.");

        if (this.isAlive()) {
            var score = Objects.requireNonNull(this.getPlayer()).getScore();
            this.setResultScore(Math.round(score));

            var xpModifier = this.difficulty.getPlainModifier() / 4.0;
            this.game.addXp(score * xpModifier);
        }

        this.gameOverTime = Instant.now();
        this.gameOver = true;
        this.gamemode.onGameOver();

        PlayerEvents.GAME_OVER.factory().onGameOver(this, this.game.player, this.gameOverTime);

        BubbleBlaster.invokeAndWait(() -> {
            this.game.showScreen(new GameOverScreen(this.getResultScore(), title));
        });
        this.save();
        return true;
    }

    public double getLocalDifficulty() {
        var diff = this.getDifficulty();

        var value = this.difficultyModifiers.modify(diff);
        if (!Constants.DIFFICULTY_EFFECT_TYPE.isLocal()) {
            return value;
        }

        this.stateDifficultyModifier = Comparison.max(new ArrayList<>(this.stateDifficultyModifiers.values()), 1f);
        if (this.getPlayer() == null) return value * this.stateDifficultyModifier;

        var i = (this.getPlayer().getLevel() - 1) * 5 + 1;
        return i * value * this.stateDifficultyModifier;
    }

    public float getStateDifficultyModifier() {
        return this.stateDifficultyModifier;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void setGlobalBubbleSpeedModifier(float speedModifier) {
        this.globalBubbleSpeedModifier = speedModifier;
    }

    public float getGlobalBubbleSpeedModifier() {
        return this.bubblesFrozen ? 0 : this.globalBubbleSpeedModifier;
    }

    public void setBubblesFrozen(boolean bubblesFrozen) {
        this.bubblesFrozen = bubblesFrozen;
    }

    public boolean isBubblesFrozen() {
        return this.bubblesFrozen;
    }

    public boolean isGameplayEventActive(GameplayEvent gameplayEvent) {
        return this.gameplayEventActive.contains(gameplayEvent);
    }

    private GameplayContext createGameplayContext() {
        return new GameplayContext(Instant.now(), this, this.gamemode, this.gameplayStorage);
    }

    @Deprecated
    public void addGameStateActive(GameplayEvent gameplayEvent) {
        this.gameplayEventActive.add(gameplayEvent);
    }

    @Deprecated
    public void removeGameStateActive(GameplayEvent gameplayEvent) {
        this.gameplayEventActive.remove(gameplayEvent);
    }

    @Deprecated
    public boolean isBloodMoonActive() {
        return this.activeEvent instanceof BloodMoonGameplayEvent;
    }

    public boolean isEventActive(GameplayEvent event) {
        return this.activeEvent == event;
    }

    public boolean isAnyEventActive() {
        return this.activeEvent != null;
    }

    public GameplayStorage getGameplayStorage() {
        return this.gameplayStorage;
    }

    public void tickBloodMoon() {
        var loadedGame = BubbleBlaster.getInstance().getLoadedGame();
        if (loadedGame == null) {
            return;
        }

        if (--this.nextBloodMoon < 0) {
            this.nextBloodMoon = -1;
            this.beginEvent(GameplayEvents.BLOOD_MOON_EVENT);
        }
    }

    @Deprecated(forRemoval = true)
    public void triggerBloodMoon() {
        this.beginEvent(GameplayEvents.BLOOD_MOON_EVENT);
    }

    @CanIgnoreReturnValue
    public boolean beginEvent(GameplayEvent event) {
        if (!isOnTickingThread())
            throw new IllegalCallerException("Beginning a gameplay event should be on ticking thread.");

        if (this.isAnyEventActive()) return false;

        this.activeEvent = event;
        event.begin(this);
        return true;
    }

    @CanIgnoreReturnValue
    public boolean endEvent(GameplayEvent event) {
        if (!isOnTickingThread())
            throw new IllegalCallerException("Ending gameplay event should be on ticking thread.");

        if (this.activeEvent != event)
            return false;

        this.activeEvent = null;
        event.end(this);
        return true;
    }

    public void endEvent() {
        if (!isOnTickingThread())
            throw new IllegalCallerException("Ending gameplay event should be on ticking thread.");

        var event = this.activeEvent;
        this.activeEvent = null;
        event.end(this);
    }

    @Deprecated(forRemoval = true)
    public void stopBloodMoon() {
        if (!this.isBloodMoonActive())
            return;

        GameplayEvents.BLOOD_MOON_EVENT.deactivate();
        BubbleBlaster.getInstance().gameplayMusic.next();
    }

    public long getResultScore() {
        return this.resultScore;
    }

    public void setResultScore(long resultScore) {
        this.resultScore = resultScore;
    }

    
    public long getTicks() {
        return this.ticks;
    }

    /**
     * Get a Random Bubble
     * Gets a random bubble from the bubble system.
     *
     * @return The bubble type.
     * @see BubbleSystem#random(RandomSource, World)
     */
    
    public BubbleType getRandomBubble(RandomSource random) {
        var bubbleType = this.gamemode.randomBubble(random, this);
        if (bubbleType != null)
            return bubbleType;

        bubbleType = BubbleSystem.random(random, this);

        var retries = 0;
        while (bubbleType == null) {
            bubbleType = BubbleSystem.random(random, this);
            if (++retries == 5)
                return this.gamemode.getDefaultBubble();
        }

        var canSpawn = bubbleType.canSpawn(this);

        if (canSpawn) return bubbleType;
        return this.gamemode.getDefaultBubble();
    }

    public void attack(Entity target, double damage, EntityDamageSource damageSource) {
        if (target instanceof LivingEntity) {
            var e = (LivingEntity) target;
            e.damage(damage, damageSource);
        }
    }

    public Difficulty getDifficulty() {
        return this.difficulty;
    }

    public Difficulty.ModifierMap getDifficultyModifiers() {
        return this.difficultyModifiers;
    }

    public void setStateDifficultyModifier(GameplayEvent gameplayEvent, float modifier) {
        this.stateDifficultyModifiers.put(gameplayEvent, modifier);
    }

    public void removeStateDifficultyModifier(GameplayEvent gameplayEvent) {
        this.stateDifficultyModifiers.remove(gameplayEvent);
    }

    public Object getStateDifficultyModifier(GameplayEvent gameplayEvent) {
        return this.stateDifficultyModifiers.get(gameplayEvent);
    }

    /**
     * Get all entities currently spawned.
     *
     * @return all the entities.
     */
    public Collection<Entity> getEntities() {
        return Collections.unmodifiableCollection(this.entitiesById.values());
    }

    /**
     * Spawn entity from loading.
     *
     * @param entityData the data create the entity to spawn.
     */
    public void spawnEntityFromState(MapType entityData) {
        if (!BubbleBlaster.isOnTickingThread()) {
            BubbleBlaster.invokeTick(() -> this.loadAndSpawnEntity(entityData));
            return;
        }
        this.loadAndSpawnEntity(entityData);
    }

    private void loadAndSpawnEntity(MapType data) {
        var type = data.getString("Type");
        var entityType = Registries.ENTITIES.getValue(Identifier.parse(type));
        var entity = entityType.create(this, data);
        entity.preSpawn(SpawnInformation.loadingSpawn(data));
        entity.load(data);

        this.addEntity(entity);
    }

    /**
     * Runs the function only every tick given.<br>
     * Note: this isn't reliable off the ticking thread.
     *
     * @param ticks the #th tick to run the function on.
     * @param func  the function to run.
     */
    public void onlyTickEvery(long ticks, Runnable func) {
        if (this.ticks % ticks == 0L) {
            func.run();
        }
    }

    /**
     * Spawn an entity into the world.
     *
     * @see Spawn#onSpawn(Entity, SpawnInformation)
     *
     * @param entity      the entity to spawn.
     * @param information the spawn information
     */
    public void spawn(Entity entity, SpawnInformation information) {
        BubbleBlaster.invokeTick(() -> {
            if (EntityEvents.SPAWN.factory().onSpawn(entity, information).isCanceled()) {
                return;
            }

            if (information.getReason() instanceof NaturalSpawnReason
                    && ((NaturalSpawnReason) information.getReason()).getUsage() == SpawnUsage.BUBBLE_SPAWN && this.bubblesFrozen) {
                var reason = (NaturalSpawnReason) information.getReason();
                return;
            }

            // Prepare entity with spawn information,
            entity.preSpawn(information);

            var pos = entity.getPos();
            entity.onSpawn(pos, this);

            this.addEntity(entity);
        });
    }

    private long nextId() {
        return this.nextEntityId++;
    }

    /**
     * Spawn an entity into the world.
     *
     * @param type        the entity to spawn (by type). This uses {@link EntityType#create(World, MapType)}
     * @param information the spawn information.
     * @param <T>         the type of entity.
     * @return the spawned entity
     *
     * @see EntityType#create(World, MapType)
     * @see SpawnInformation#getData()
     * @see World#spawn(Entity, SpawnInformation)
     */
    public <T extends Entity> T spawn(EntityType<T> type, SpawnInformation information) {
        var entity = type.create(this, information.getData());
        this.spawn(entity, information);
        return entity;
    }

    /**
     * @return the game type bound to this world.
     */
    public Gamemode getGamemode() {
        return this.gamemode;
    }

    /**
     * Check if the world is initialized.
     *
     * @return true if initialized.
     */
    public boolean isInitialized() {
        return this.initialized;
    }

    /**
     * Tick the world.<br>\
     */
    @ApiStatus.Internal
    public void tick() {
        if (this.initialized) {
            entities:
            {
                if (!this.entitiesLock.tryLock()) break entities;

                // Tick entities
                List<Entity> toRemove = new ArrayList<>();
                for (var entity : this.entitiesById.values()) {
                    if (entity.willBeDeleted()) {
                        toRemove.add(entity);
                        continue;
                    }

                    if (entity instanceof Player) {
                        var p = (Player) entity;
                        TickEvents.PRE_TICK_PLAYER.factory().onTickPlayer(p);
                    }
                    TickEvents.PRE_TICK_ENTITY.factory().onTickEntity(entity);
                    entity.tick(this);
                    TickEvents.POST_TICK_ENTITY.factory().onTickEntity(entity);
                    if (entity instanceof Player) {
                        var p = (Player) entity;
                        TickEvents.POST_TICK_PLAYER.factory().onTickPlayer(p);
                    }
                }

                for (var entity : toRemove) {
                    this.entitiesById.remove(entity.getId());
                    this.entitiesByUuid.remove(entity.getUuid());
                }

                this.entitiesLock.unlock();
            }

            this.tickSpawning();
            this.tickBloodMoon();

            // Tick gameplay events
            gamePlayEvent:
            {
                if (this.activeEvent != null) {
                    if (!this.activeEvent.shouldContinue(this.createGameplayContext())) {
                        if (!WorldEvents.GAMEPLAY_EVENT_DEACTIVATED.factory().onGameplayEventDeactivated(this, this.activeEvent).isCanceled()) {
                            this.endEvent();
                        }
                    }

                    var gameplayEvent = this.activeEvent;
                    if (gameplayEvent != null) {
                        gameplayEvent.tick();
                    }

                    break gamePlayEvent;
                }

                this.onlyTickEvery(5, () -> {
                    var choices = Randomizer.choices(Registries.GAMEPLAY_EVENTS.values(), 3);
                    for (var gameplayEvent : choices) {
                        if (gameplayEvent.shouldActivate(this.createGameplayContext())) {
                            if (!WorldEvents.GAMEPLAY_EVENT_TRIGGERED.factory().onGameplayEventTriggered(this, gameplayEvent).isCanceled()) {
                                this.activeEvent = gameplayEvent;
                                this.activeEvent.begin(this);

                                break;
                            }
                        }
                    }
                });
            }

            // Advance ticks
            this.ticks++;
        }
    }

    private void tickSpawning() {
        if (this.bubblesFrozen || this.gamemode instanceof OpenWorldMode) {
            return;
        }
        if (this.entitiesById.values().stream().filter(Bubble.class::isInstance).count() < this.maxBubbles) {
            var idx = this.entitySeedIdx++;
            RandomSource random = new JavaRandom(this.seed ^ idx);
            var variant = Bubble.getRandomVariant(this, random.nextRandom());

            final var retry = 0;
            BubbleSpawnContext.inContext(random, retry, () -> {
                var bubble = new Bubble(this, variant);
                this.spawn(bubble, SpawnInformation.naturalSpawn(null, random, SpawnUsage.BUBBLE_SPAWN, retry, this));
                return bubble;
            });
        }
    }

    public void gameOver(Player player) {
        synchronized (this.entitiesLock) {
            this.entitiesById.remove(player.getId());
        }
    }

    public void joinPlayer(Player player) {
        this.entitiesLock.lock();
        this.players.add(player);
        this.addEntityUnlocked(player);
        this.entitiesLock.unlock();
    }

    public <T extends Entity> Collection<T> getEntitiesByClass(Class<T> bubbleEntityClass) {
        synchronized (this.entitiesLock) {
            return this.entitiesById.values().stream()
                    .filter(entity -> bubbleEntityClass.isAssignableFrom(entity.getClass()))
                    .map(bubbleEntityClass::cast).collect(Collectors.toList());
        }
    }

    public GameplayEvent getActiveEvent() {
        return this.activeEvent;
    }

    @Override
    public void close() {
        this.shuttingDown = true;

        WorldEvents.WORLD_STOPPING.factory().onWorldStopping(this);

        synchronized (this.entitiesLock) {
            for (var entity : this.entitiesById.values()) {
                entity.delete();
            }
        }

        WorldEvents.WORLD_STOPPED.factory().onWorldStopped(this);
    }

    public void setCurrentGameEvent(GameplayEvent currentGameplayEvent) {
        this.activeEvent = currentGameplayEvent;
    }

    public long getSeed() {
        return this.seed;
    }

    
    public  boolean isGameOver() {
        return this.gameOver;
    }

    public boolean isAlive() {
        return !this.gameOver;
    }

    public Player getPlayer() {
        return this.game.player;
    }

    public BubbleBlaster game() {
        return this.game;
    }

    public GameSave getGameSave() {
        return this.gameSave;
    }

    public void prepareCreation(GameSave save) throws IOException {
        save.createFolders("registries");
    }

    
    public Entity getEntityAt(Vector2 pos) {
        for (var entity : this.entitiesById.values()) {
            if (entity.getShape().contains(pos)) {
                return entity;
            }
        }

        return null;
    }

    public void onLevelUp(Player player, int to) {
        if (player == this.game.player) {
            this.gamemode.onLevelUp(player, to);
            HudType.getCurrent().onLevelUp(to);
        }
    }

    
    public Entity getNearestEntity(Vector2D pos) {
        var distance = Double.MAX_VALUE;
        Entity nearest = null;
        for (var entity : this.entitiesById.values()) {
            var cur = entity.distanceTo(pos);
            if (cur < distance) {
                distance = cur;
                nearest = entity;
            }
        }
        return nearest;
    }

    public Entity getNearestEntity(Vector2D pos, EntityType<?> targetType) {
        var distance = Double.MAX_VALUE;
        Entity nearest = null;
        for (var entity : this.entitiesById.values()) {
            if (!entity.getType().equals(targetType)) continue;
            var cur = entity.distanceTo(pos);
            if (cur < distance) {
                distance = cur;
                nearest = entity;
            }
        }
        return nearest;
    }

    public void freezeBubblesSecs(int seconds) {
        this.freezeBubbles(seconds * TPS);
    }

    public void freezeBubbles(int ticks) {
        this.bubblesFrozenTicks = ticks;
        this.bubblesFrozen = true;
    }

    public boolean isSaving() {
        return this.saveLock.isLocked();
    }

    public void dispose() {
        this.gamemode.end();
        this.entitiesById.clear();
    }

    public void annihilate() {
        this.gamemode.end();
        this.entitiesById.clear();
        this.players.clear();
        this.game.player = null;
    }

    @Override
    public void fillInCrash(CrashLog crashLog) {
        var category = new CrashCategory("World", new RuntimeException());
        category.add("Players", this.players);
        category.add("Entities", this.entitiesById);
        category.add("Game Save", this.gameSave);
        category.add("Cur. Gameplay Event", this.activeEvent);
        crashLog.addCategory(category);
    }

    
    public Instant getGameOverTime() {
        return this.gameOverTime;
    }

    public MusicEvent getMusic() {
        return this.music;
    }

    public int getNextBloodMoon() {
        return this.nextBloodMoon;
    }
}
