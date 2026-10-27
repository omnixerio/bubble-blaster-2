package dev.ultreon.bubbles;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.crashinvaders.vfx.effects.VfxEffect;
import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.ultreon.bubbles.audio.MusicEvent;
import dev.ultreon.bubbles.audio.MusicSystem;
import dev.ultreon.bubbles.audio.SoundInstance;
import dev.ultreon.bubbles.common.Difficulty;
import dev.ultreon.bubbles.common.GameFolders;
import dev.ultreon.bubbles.common.exceptions.FontLoadException;
import dev.ultreon.bubbles.common.exceptions.ResourceNotFoundException;
import dev.ultreon.bubbles.data.DataKeys;
import dev.ultreon.bubbles.data.GlobalSaveData;
import dev.ultreon.bubbles.debug.DebugRenderer;
import dev.ultreon.bubbles.debug.Profiler;
import dev.ultreon.bubbles.debug.ThreadSection;
import dev.ultreon.bubbles.entity.player.Player;
import dev.ultreon.bubbles.event.v1.*;
import dev.ultreon.bubbles.gamemode.Gamemode;
import dev.ultreon.bubbles.init.*;
import dev.ultreon.bubbles.input.*;
import dev.ultreon.bubbles.notification.Notification;
import dev.ultreon.bubbles.notification.Notifications;
import dev.ultreon.bubbles.player.InputController;
import dev.ultreon.bubbles.random.JavaRandom;
import dev.ultreon.bubbles.random.RandomSource;
import dev.ultreon.bubbles.random.valuesource.RandomValueSource;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.bubbles.render.*;
import dev.ultreon.bubbles.render.gui.screen.*;
import dev.ultreon.bubbles.save.GameSave;
import dev.ultreon.bubbles.world.World;
import dev.ultreon.bubbles.world.WorldRenderer;
import dev.ultreon.libs.collections.v0.maps.OrderedHashMap;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.libs.commons.v0.Mth;
import dev.ultreon.libs.commons.v0.ProgressMessenger;
import dev.ultreon.libs.commons.v0.util.IllegalCallerException;
import dev.ultreon.libs.crash.v0.ApplicationCrash;
import dev.ultreon.libs.crash.v0.CrashLog;
import dev.ultreon.libs.datetime.v0.Duration;
import dev.ultreon.libs.registries.v0.Registry;
import dev.ultreon.bubbles.resources.ResourceManager;
import dev.ultreon.bubbles.lang.LanguageManager;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import oshi.annotation.concurrent.NotThreadSafe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The Bubble Blaster game main class.
 *
 * @since 0.0.1
 */
@ParametersAreNonnullByDefault
@SuppressWarnings({"ResultOfMethodCallIgnored", "RedundantSuppression"})
public final class BubbleBlaster extends ApplicationAdapter implements CrashFiller {
    public static final int TPS = 40;
    public static final String NAMESPACE = "bubbleblaster";

    // Logger.
    public static final Logger LOGGER = GamePlatform.get().getLogger("Generic");
    public static final Instant BOOT_TIME = Instant.now();
    public static final Random RANDOM = new Random();
    private static final String FATAL_ERROR_MSG = "FATAL: Can't handle crash log.";

    // Modes
    private static boolean debugMode;
    // Initial game information / types.
    private static ClassLoader classLoader;
    // Number values.
    private static long ticks = 0L;
    // Instance
    private static BubbleBlaster instance;

    private static FileHandle dataDir;

    // Public Game Components
    public final Notifications notifications;
    public final Profiler profiler = new Profiler();

    // Tasks
    public final Cursor arrowCursor;
    public final Cursor handCursor;
    public final RandomSource random = new JavaRandom();

    // Fonts.
    private FontSet sansFont;
    private final String fontName;

    public final ScheduledExecutorService schedulerService = Executors.newScheduledThreadPool(Math.max(Runtime.getRuntime().availableProcessors() / 4, 2));
    private final ResourceManager resourceManager;
    private final GameWindow window;
    private Screen screen;
    private final RenderSettings renderSettings;
    // Rendering
    private final DebugRenderer debugRenderer;
    private WorldRenderer worldRenderer;
    // Managers.
    private final TextureManager textureManager;

    // Misc
    private final Ticker ticker = new Ticker();

    // Utility objects.
    public InputController input;

    // World
    @Nullable
    public World world;

    // Player entity
    public Player player;

    // Values
    @IntRange(from = 0) int fps;
    private int currentTps;

    // Game states.
    private boolean loaded;

    // Running value.
    private volatile boolean running = false;

    // Threads
    private static Thread renderingThread;
    private static Thread tickingThread;
    private final GarbageCollector garbageCollector = new GarbageCollector();
    float gameFrameTime;
    private boolean stopping;
    private BitmapFont monospaceFont;
    private BitmapFont pixelFont;
    private BitmapFont logoFont;

    // Loaded game.
    @Nullable
    private LoadedGame loadedGame;
    private final ManualCrashOverlay manualCrashOverlay = new ManualCrashOverlay();
    private GlitchRenderer glitchRenderer = null;
    private boolean isGlitched = false;
    private final Map<Thread, ThreadSection> lastProfile = new HashMap<>();
    private float fadeInDuration = Float.NEGATIVE_INFINITY;
    private boolean fadeIn = false;
    private long fadeInStart = 0L;
    private final GlobalSaveData globalSaveData = GlobalSaveData.instance();
    private final OrthographicCamera camera;
    public final Viewport viewport;
    private Renderer currentRenderer;
    private final Renderer renderer;
    private final Map<UUID, Runnable> afterLoading = new OrderedHashMap<>();
    private long lastTickMs;
    private boolean isTickThreadDead;
    private final SpriteBatch batch;
    private final List<MusicEvent> menuMusicList = Lists.newArrayList(MusicEvents.SUBMARINE);
    private final List<MusicEvent> gameplayMusicList = Lists.newArrayList(MusicEvents.SUBMARINE);
    public final MusicSystem gameplayMusic = new MusicSystem(RandomValueSource.random(3.0, 6.0), RandomValueSource.random(40.0, 80.0), this.gameplayMusicList);
    public final MusicSystem menuMusic = new MusicSystem(RandomValueSource.random(1.0, 3.0), RandomValueSource.random(5.0, 10.0), this.menuMusicList);
    private final ShapeRenderer shapes;
    private InputType currentInput = InputType.KeyboardAndMouse;
    public final DesktopInputHandler desktopInput;
    public final ControllerInputHandler controllerInput;
    public final MobileInputHandler mobileInput;
    public static PollingExecutorService renderExecutor;
    public static PollingExecutorService tickExecutor;
    private boolean unsupportedGL = false;
    private BitmapFont errorFont;
    private double money;
    private double xp;
    private int xpLevel;
    private final List<FreeTypeFontGenerator> fontGenerators = new ArrayList<>();
    private final List<PixmapPacker> fontPackers = new ArrayList<>();

    private BubbleBlaster() {
        if (instance != null)
            throw new UnsupportedOperationException("Can't open the game twice.");

        ShaderProgram.pedantic = false;

        renderingThread = Thread.currentThread();

        renderExecutor = new PollingExecutorService();

        this.batch = new SpriteBatch(1000);
        this.shapes = new ShapeRenderer(5000);
        if (Gdx.gl20 == null) {
            this.errorFont = this.loadFont(new Identifier("roboto/roboto_mono_bold"), 40);
            this.unsupportedGL = true;
        }

        this.arrowCursor = this.createCursor("textures/cursor/arrow.png", 0, 0);
        this.handCursor = this.createCursor("textures/cursor/pointer.png", 10, 10);

        instance = this;
        this.notifications = new Notifications();

        Gdx.app.setApplicationLogger(new PlatformLogger());

        HdpiUtils.setMode(HdpiMode.Logical);

        GameFolders.CONFIG_DIR.mkdirs();

        GamePlatform.get().initImGui();

        // Set game input processor for LibGDX
        Gdx.input.setInputProcessor(new DesktopInput());
        Controllers.addListener(new ControllerInput());

        this.desktopInput = new DesktopInputHandler();
        this.mobileInput = new MobileInputHandler();
        this.controllerInput = new ControllerInputHandler();

        // Set HiDpi mode
        HdpiUtils.setMode(HdpiMode.Pixels);

        this.textureManager = TextureManager.instance();
        this.debugRenderer = new DebugRenderer(this);
        this.camera = new OrthographicCamera();
        this.viewport = new ScreenViewport(this.camera);

        this.shapes.setAutoShapeType(true);
        this.renderer = new Renderer(this.shapes, this.batch, this.camera);

        // Set default uncaught exception handler.
        Thread.setDefaultUncaughtExceptionHandler(new GameExceptions());
        Thread.currentThread().setUncaughtExceptionHandler(new GameExceptions());

        // Hook output for logger.
        System.setErr(new RedirectPrintStream(GamePlatform.get().getLogger("STDERR")));
        System.setOut(new RedirectPrintStream(GamePlatform.get().getLogger("STDOUT")));

        // Assign instance.
        this.window = GameWindow.create(new GameWindow.Properties("Bubble Blaster", 1280, 720).close(this::dispose));

        // Prepare for game launch
        this.resourceManager = new ResourceManager("assets");
        this.renderSettings = new RenderSettings();

        GamePlatform.get().initMods();

        DebugFormatters.initClass();

        // Prepare for loading.
        this.prepare();

        this.sansFont = this.loadFont(BubbleBlaster.id("noto_sans/noto_sans_regular"), 16);

        BubbleTypes.register();
        AmmoTypes.register();
        Entities.register();
        Fonts.register();
        SoundEvents.register();
        MusicEvents.register();
        HudTypes.register();
        StatusEffects.register();
        Abilities.register();
        GameplayEvents.register();
        Gamemodes.register();
        TextureCollections.register();

        // Load game with loading screen.
        this.load(new ProgressMessenger(this::log, 1000));

        BubbleBlaster.newActivity(GameActivity.LOADING);

        LOGGER.info("Discord RPC is initializing!");

        // Set world renderer
        this.worldRenderer = new WorldRenderer();

        try {
            var resource = this.getClass().getClassLoader().getResource(".resource-root");
            if (resource != null) {
                var filePath = BubbleBlaster.getGamePath(resource);

                LOGGER.debug("Valid game path: {}", filePath);

                this.resourceManager.importPackage(filePath);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Logs directory creation.
        GameFolders.LOGS_DIR.mkdirs();

        // Font Name
        this.fontName = "Chicle Regular";

        // Register events.
        InputEvents.KEY_PRESS.listen(this::onKeyPress);
        InputEvents.MOUSE_CLICK.listen(this::onMouseClick);

        GameEvents.CLIENT_STARTED.factory().onClientStarted(this);

        // Start scene-manager.
        try {
            this.showScreen(new SplashScreen(), true);

            RpcHandler.enable();
        } catch (Throwable t) {
            var crashLog = new CrashLog("Oops, game crashed!", t);
            BubbleBlaster.crash(crashLog.createCrash());
        }

        if (!this.window.isFocused()) {
            this.window.requestUserAttention();
        }
    }

    public static BitmapFont createBitmapFont(Identifier id, int size) {
        return instance.loadFont(BubbleBlaster.resource(id.mapPath(path -> "fonts/" + path + ".ttf")), size);
    }

    private Cursor createCursor(String path, int hotspotX, int hotspotY) {
        var pixmap = new Pixmap(BubbleBlaster.resource(BubbleBlaster.id(path)));
        return Gdx.graphics.newCursor(pixmap, hotspotX, hotspotY);
    }

    @CanIgnoreReturnValue
    public static <T> T invokeAndWait(Callable<@NotNull T> func) {
        return renderExecutor.submit(func).join();
    }

    public static void invokeAndWait(Runnable func) {
        renderExecutor.submit(func).join();
    }

    @CanIgnoreReturnValue
    public static @NotNull CompletableFuture<Void> invoke(Runnable func) {
        return renderExecutor.submit(func);
    }

    @CanIgnoreReturnValue
    public static <T> @NotNull CompletableFuture<T> invoke(Callable<T> func) {
        return renderExecutor.submit(func);
    }

    @CanIgnoreReturnValue
    public static <T> T invokeTickAndWait(Callable<@NotNull T> func) {
        return tickExecutor.submit(func).join();
    }

    public static void invokeTickAndWait(Runnable func) {
        tickExecutor.submit(func).join();
    }

    @CanIgnoreReturnValue
    public static @NotNull CompletableFuture<Void> invokeTick(Runnable func) {
        return tickExecutor.submit(func);
    }

    @CanIgnoreReturnValue
    public static <T> @NotNull CompletableFuture<T> invokeTick(Callable<T> func) {
        return tickExecutor.submit(func);
    }

    public static Instant getBootTime() {
        return BOOT_TIME;
    }

    public static void whenLoaded(UUID id, Runnable func) {
        if (!BubbleBlaster.isOnRenderingThread()) {
            BubbleBlaster.invoke(() -> BubbleBlaster.whenLoaded(id, func));
            return;
        }

        if (instance.isLoaded()) {
            func.run();
            return;
        }

        if (!instance.afterLoading.containsKey(id)) {
            instance.afterLoading.put(id, func);
        }
    }

    public static FileHandle data(String path) {
        return dataDir.child(path);
    }

    public static FileHandle getDataDir() {
        return dataDir;
    }

    public static FileHandle resource(Identifier id) {
        return Gdx.files.internal("assets/" + id.location() + "/" + id.path());
    }

    @NotThreadSafe
    public static Sound newSound(Identifier resourceId) {
        if (!BubbleBlaster.isOnRenderingThread())
            throw new IllegalCallerException("Creating a sound object should be on the rendering thread!");

        var resource = BubbleBlaster.resource(resourceId);
        if (!resource.exists()) throw new ResourceNotFoundException(resourceId);

        return Gdx.audio.newSound(resource);
    }

    @NotThreadSafe
    public static Music newMusic(Identifier resourceId) {
        if (!BubbleBlaster.isOnRenderingThread())
            throw new IllegalCallerException("Creating a music object should be on the rendering thread.");

        var resource = BubbleBlaster.resource(resourceId);
        if (!resource.exists()) throw new ResourceNotFoundException(resourceId);

        return Gdx.audio.newMusic(resource);
    }

    public static FileHandle getConfigDir() {
        return BubbleBlaster.data("config/");
    }

    private static Path getGamePath(URL resource) throws IOException, URISyntaxException {
        var protocol = resource.getProtocol();

        Path filePath;

        switch (protocol) {
            case "jar":
                var jarURLConnection = (JarURLConnection) resource.openConnection();
                var fileUrl = jarURLConnection.getJarFileURL();
                if (!fileUrl.getProtocol().equals("file")) {
                    throw new Error("Jar file isn't local.");
                }
                filePath = Paths.get(fileUrl.toURI());
                break;
            case "file":
                filePath = Paths.get(resource.toURI()).getParent();
                break;
            default:
                throw new Error("Illegal protocol: " + resource.getProtocol());
        }
        return filePath;
    }

    public void pauseGame() {
        if (this.isInGame() && !(this.getCurrentScreen() instanceof PauseScreen)) {
            this.showScreen(new PauseScreen());
            this.gameplayMusic.pause();
        }
    }

    public void resumeGame() {
        if (this.isInGame() && this.getCurrentScreen() instanceof PauseScreen) {
            this.showScreen(null);
            this.gameplayMusic.play();
        }
    }

    @Override
    public void resize(int width, int height) {
        var logicalWidth = Constants.DEFAULT_SIZE.x;
        var logicalHeight = Constants.DEFAULT_SIZE.y;
        var gameViewport = this.window.getGameViewport();

        this.camera.setToOrtho(true, logicalWidth, logicalHeight);
        this.renderer.resize(logicalWidth, logicalHeight);
        this.viewport.update(logicalWidth, logicalHeight);
        Gdx.gl.glViewport(
                HdpiUtils.toBackBufferX(Math.round(gameViewport.x)),
                HdpiUtils.toBackBufferY(Math.round(gameViewport.y)),
                HdpiUtils.toBackBufferX(Math.round(gameViewport.width)),
                HdpiUtils.toBackBufferY(Math.round(gameViewport.height))
        );

        var glitch = this.glitchRenderer;
        if (glitch != null)
            glitch.resize(width, height);

        var screen = this.screen;
        if (screen != null)
            screen.resize(logicalWidth, logicalHeight);
    }

    @Override
    public void render() {
        if (this.unsupportedGL) {
            this.camera.update();

            HdpiUtils.setMode(HdpiMode.Logical);
            ScreenUtils.clear(Colors.CRIMSON);
            this.batch.begin();
            this.errorFont.setColor(1, 1, 1, 1);
            this.errorFont.draw(this.batch, "Unsupported GL version!", 50, 50);
            this.errorFont.draw(this.batch, "This game requires GL 2.0 or higher.", 50, 50 + this.errorFont.getLineHeight());
            this.batch.end();
            return;
        }
        try {
            if (Gdx.graphics.getFrameId() == 2) {
                this.firstRender();
            }
            this.camera.update();

            this.renderer.begin();
            this.currentRenderer = this.renderer;

            if (this.screen == null) {
                this.renderer.hideCursor();
            }

            if (this.isLoaded()) {
                if (Instant.now().isAfter(this.getLastTickTime().plusSeconds(60)) && !this.isTickThreadDead) {
                    this.markTickingDead();
                    this.notifications.notify(
                            Notification.builder("Game Ticking", "Game ticking hasn't happened in 60 secs!")
                                    .subText("Watchdog")
                                    .duration(Duration.ofSeconds(5))
                                    .build()
                    );
                } else if (this.isTickThreadDead) {
                    this.isTickThreadDead = false;
                    this.notifications.notify(
                            Notification.builder("Game Ticking", "Game ticking came back!")
                                    .subText("Watchdog")
                                    .duration(Duration.ofSeconds(5))
                                    .build()
                    );
                }
            }

            try {
                var mousePos = DesktopInput.getMousePos();
                var mouseX = (int) mousePos.x;
                var mouseY = (int) mousePos.y;

                var deltaTime = Gdx.graphics.getDeltaTime();

                renderExecutor.pollAll();

                this.profiler.section("renderGame", () -> this.renderGame(this.renderer, mouseX, mouseY, this.gameFrameTime));

                if (BubbleBlaster.isDebugMode() || GamePlatform.get().isDebugGuiOpen()) {
                    this.debugRenderer.render(this.renderer);
                }

                this.fps = Gdx.graphics.getFramesPerSecond();
                this.notifications.render(this.renderer, mouseX, mouseY, deltaTime);

                GamePlatform.get().renderImGui(this.renderer);

                if (this.isGlitched && BubbleBlasterConfig.ENABLE_ANNOYING_EASTER_EGGS.get()) {
                    this.glitchRenderer.render(this.renderer);
                } else {
                    this.manualCrashOverlay.render(this.renderer, mouseX, mouseY, deltaTime);
                }
            } catch (OutOfMemoryError error) {
                this.outOfMemory(error);
            }

            this.currentRenderer = null;
            this.renderer.end();
        } catch (Exception e) {
            BubbleBlaster.crash(e);
        }
    }

    private void markTickingDead() {
    }

    private void firstRender() {
        BubbleBlasterConfig.reload();
        this.window.setVisible(true);
        this.window.setFullscreen(BubbleBlasterConfig.FULLSCREEN.get());

        var mousePos = this.windowToGame(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        this.mouseMove((int) mousePos.x, (int) mousePos.y);
    }

    @SuppressWarnings("ConstantValue")
    @Override
    @ApiStatus.Internal
    public void dispose() {
        this.running = false;

        GamePlatform.get().dispose();

        Controllers.clearListeners();

        // Shut-down game.
        LOGGER.info("Shutting down Bubble Blaster");

        if (tickingThread != null) {
            tickingThread.interrupt();
        }

        final var loadedGame = this.loadedGame;
        if (loadedGame != null) {
            loadedGame.end();
        }

        final var world = this.world;
        if (world != null) {
            world.close();
        }

        SoundInstance.stopAll();

        try {
            if (this.garbageCollector != null) {
                this.garbageCollector.shutdown();
            }
            if (BubbleBlaster.tickingThread != null) {
                BubbleBlaster.tickingThread.interrupt();
                BubbleBlaster.tickingThread.join(1000);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to stop threads:", e);
            return;
        }
        this.fontGenerators.forEach(FreeTypeFontGenerator::dispose);
        this.fontPackers.forEach(PixmapPacker::dispose);
        this.checkForExitEvents();
    }

    public static Map<Thread, ThreadSection> getLastProfile() {
        return instance.lastProfile;
    }

    public static boolean hasRendered() {
        return Gdx.graphics.getFrameId() >= 2;
    }

    public static BubbleBlaster getInstance() {
        return instance;
    }

    public static Identifier id(String path) {
        return new Identifier(NAMESPACE, path);
    }

    /**
     * Launch method.
     * Contains argument parsing.
     *
     * @return the bubble blaster instance.
     */
    @ApiStatus.Internal
    static BubbleBlaster launch(GamePlatform platform) {
        System.setProperty("log4j2.formatMsgNoLookups", "true"); // Fix CVE-2021-44228 exploit.
        Identifier.setDefaultNamespace(NAMESPACE);
        ResourceManager.logger = BubbleBlaster.getLogger("ResourceManager");
        Registry.dumpLogger = BubbleBlaster.getLogger("RegistryDump");
        LanguageManager.INSTANCE.logger = BubbleBlaster.getLogger("LanguageManager");

        // Get game-directory.
        dataDir = platform.getDataDirectory();
        debugMode = platform.isDebug();

        BubbleBlaster.classLoader = BubbleBlaster.getClassLoader();

        return new BubbleBlaster();
    }

    private static dev.ultreon.libs.commons.v0.Logger getLogger(String name) {
        var logger = GamePlatform.get().getLogger(name);
        return (level, message, t) -> {
            switch (level) {
                case ERROR:
                    logger.error(message, t);
                    break;
                case WARN:
                    logger.warn(message, t);
                    break;
                case INFO:
                    logger.info(message, t);
                    break;
                case DEBUG:
                    logger.debug(message, t);
                    break;
            }
        };
    }

    private static String getAppData() {
        return null;
    }

    /////////////////////////
    //     Game values     //
    /////////////////////////
    public static long getTicks() {
        return ticks;
    }

    ///////////////////////////
    //     Startup Modes     //
    ///////////////////////////
    public static boolean isDebugMode() {
        return debugMode;
    }

    @Deprecated
    public static boolean isDevEnv() {
        return GamePlatform.get().isDevelopmentEnvironment();
    }

    ///////////////////////////
    //     Class Loaders     //
    ///////////////////////////
    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    /////////////////////////////
    //     Middle position     //
    /////////////////////////////
    public static float getMiddleX() {
        return (float) BubbleBlaster.getInstance().getWidth() / 2;
    }

    public static float getMiddleY() {
        return (float) BubbleBlaster.getInstance().getHeight() / 2;
    }

    public static Vector2 getMiddlePoint() {
        return new Vector2(BubbleBlaster.getMiddleX(), BubbleBlaster.getMiddleY());
    }

    /////////////////////
    //     Loggers     //
    /////////////////////
    public static Logger getLogger() {
        return LOGGER;
    }

    /////////////////////////////////////
    //     Reduce ticks to seconds     //
    /////////////////////////////////////
    public static byte reduceTicks2Secs(byte value, byte seconds) {
        return (byte) ((double) value / ((double) TPS * seconds));
    }

    public static short reduceTicks2Secs(short value, short seconds) {
        return (short) ((double) value / ((double) TPS * seconds));
    }

    public static int reduceTicks2Secs(int value, int seconds) {
        return (int) ((double) value / ((double) TPS * seconds));
    }

    public static long reduceTicks2Secs(long value, long seconds) {
        return (long) ((double) value / ((double) TPS * seconds));
    }

    public static float reduceTicks2Secs(float value, float seconds) {
        return (float) ((double) value / ((double) TPS * seconds));
    }

    public static double reduceTicks2Secs(double value, double seconds) {
        return value / ((double) TPS * seconds);
    }

    /**
     * Check if the game is paused.
     *
     * @return true if paused.
     * @see Screen#doesPauseGame()
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isPaused() {
        if (instance.world == null || instance.loadedGame == null || !instance.loaded) return true;

        var screen = instance.getCurrentScreen();
        return screen != null && screen.doesPauseGame();
    }

    public static URL getJarUrl() {
        return BubbleBlaster.class.getProtectionDomain().getCodeSource().getLocation();
    }

    public static String getFabricLoaderVersion() {
        return GamePlatform.get().getFabricLoaderVersion();
    }

    public static String getLibGDXVersion() {
        return GamePlatform.get().getLibGDXVersion();
    }

    public static String getGameVersion() {
        return GamePlatform.get().getGameVersion();
    }

    private void onKeyPress(int keyCode, boolean holding) {
        if (keyCode == Keys.F11) {
            if (!Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                return;
            }

            Gdx.graphics.setWindowedMode(1280, 720);
            return;
        }

        final var loadedGame = this.loadedGame;

        if (loadedGame != null) {
            final var world = loadedGame.getWorld();

            var player = world.getPlayer();
            if (this.canExecuteDevCommands()) {
                this.executeDevCommand(keyCode, world, player);
            }
        }

        if (this.canExecuteDevCommands() && keyCode == Keys.F8 && !holding) {
            this.renderer.triggerScissorLog();
        }
    }

    private void executeDevCommand(int keyCode, World world, Player player) {
        BubbleBlaster.invokeTick(() -> {
            switch (keyCode) {
                case Keys.F1:
                    LOGGER.warn("Triggering developer command: TRIGGER_BLOOD_MOON");
                    world.getGameplayStorage().get(NAMESPACE).putBoolean(DataKeys.BLOOD_MOON_ACTIVE, true);
                    break;
                case Keys.F3:
                    LOGGER.warn("Triggering developer command: SELF_DESTRUCT");
                    player.destroy();
                    break;
                case Keys.F4:
                    LOGGER.warn("Triggering developer command: LEVEL_UP");
                    player.levelUp();
                    break;
                case Keys.F5:
                    LOGGER.warn("Triggering developer command: SCORE_1000");
                    player.awardScore(1000);
                    break;
                case Keys.F6:
                    LOGGER.warn("Triggering developer command: RESET_HP");
                    player.setHealth(player.getMaxHealth());
                    break;
                case Keys.F7:
                    LOGGER.warn("Triggering developer command: GLITCH");
                    this.isGlitched = true;
                    break;
            }
        });
    }

    private void onMouseClick(int x, int y, int button, int clicks) {
        var loadedGame = this.loadedGame;
        if (this.canExecuteDevCommands()) {
            if (loadedGame != null && button == Buttons.LEFT) {
                if (DesktopInput.isKeyDown(Keys.F1)) {
                    Objects.requireNonNull(loadedGame.getGamemode().getPlayer()).teleport(x, y);
                }
            }
        }
    }

    @Deprecated(forRemoval = true)
    public URL getGameFile() {
        return null;
    }

    public static void newActivity(GameActivity activity) {
        RpcHandler.newActivity(activity);
    }

    private void initialGameTick() {

    }

    private boolean outOfMemory(OutOfMemoryError error) {
        System.gc();

        if (this.screen instanceof OutOfMemoryScreen) {
            BubbleBlaster.crash(error);
            return true;
        }

        if (this.world != null && !this.world.isSaving()) {
            try {
                this.world.save();
            } catch (OutOfMemoryError anotherError) {
                System.gc();
            } catch (Throwable throwable) {
                BubbleBlaster.crash(throwable);
            }
        }

        if (this.world != null) {
            this.world.annihilate();
        }
        this.world = null;
        this.worldRenderer = null;
        this.player = null;
        System.gc();

        this.showScreen(new OutOfMemoryScreen());
        return false;
    }

    private BitmapFont loadFontInternally(Identifier location) {
        return this.loadFont(Gdx.files.internal("assets/" + location.location() + "/fonts/" + location.path() + ".ttf"), 14);
    }

    public BitmapFont loadFont(FileHandle handle, int size) {
        if (!BubbleBlaster.isOnRenderingThread()) {
            var bitmapFont = BubbleBlaster.invokeAndWait(() -> this.loadFont(handle, size));
            if (bitmapFont == null) throw new FontLoadException("Render call failed");
            return bitmapFont;
        }
        var generator = new FreeTypeFontGenerator(handle);
        var packer = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 1, false);
        var parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.flip = true;
        parameter.size = size;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        parameter.incremental = true;
        parameter.packer = packer;
        try {
            var font = Objects.requireNonNull(generator.generateFont(parameter), "Font failed to generate!");
            font.setOwnsTexture(true);
            this.fontGenerators.add(generator);
            this.fontPackers.add(packer);
            return font;
        } catch (RuntimeException | Error e) {
            packer.dispose();
            generator.dispose();
            throw e;
        }
    }

    public FontSet loadFont(Identifier id) {
        return this.loadFont(id, 12);
    }

    public FontSet loadFont(Identifier id, int size) {
        return FontSet.fromIdentifier(id, size);
    }

    ////////////////////////
    //     Game flags     //
    ////////////////////////

    public void loadFonts() {
        try {
            this.sansFont = this.loadFont(BubbleBlaster.id("noto_sans/noto_sans_regular"), 16);
            this.logoFont = this.loadFont(BubbleBlaster.id("chicle"));
            this.pixelFont = this.loadFont(BubbleBlaster.id("pixel"));
            this.monospaceFont = this.loadFont(BubbleBlaster.id("roboto/roboto_mono_regular"));
        } catch (FontLoadException e) {
            BubbleBlaster.crash(e);
        }
    }

    public void gcThread() {
    }

    /**
     * Stops game-thread.
     */
    @RequiresNonNull({"ticking", "rendering", "gcThread"})
    public synchronized void stop() {
    }

    public int getScaledWidth() {
        return Constants.DEFAULT_SIZE.x;
    }

    public int getScaledHeight() {
        return Constants.DEFAULT_SIZE.y;
    }

    private void checkForExitEvents() {
        LifecycleEvents.GAME_EXIT.factory().onExit(this);
    }

    /**
     * Todo: implement save loading.
     *
     * @param saveName ...
     */
    @SuppressWarnings("EmptyMethod")
    @Deprecated
    public void loadSave(String saveName) {
        try {
            this.loadGame(GameSave.fromFile(GameFolders.SAVES_DIR.child(saveName)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Todo: implement save loading.
     *
     * @param save save to load
     */
    @Deprecated
    public void loadSave(GameSave save) {
        try {
            this.loadGame(save);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Todo: implement save creation and loading..
     *
     * @param ignoredSaveName ...
     */
    @Beta
    @SuppressWarnings("EmptyMethod")
    public void createAndLoadSave(String ignoredSaveName) {

    }

    /**
     * Display a new scene.
     *
     * @param scene the scene to display
     * @return if changing the scene was successful.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean showScreen(@Nullable Screen scene) {
        return this.showScreen(scene, false);
    }

    /**
     * Display a new scene.
     *
     * @param screen the scene to display
     * @return if changing the scene was successful.
     */
    public boolean showScreen(@Nullable Screen screen, boolean force) {
        var oldScreen = this.screen;
        if (oldScreen instanceof InternalScreen) {
            if (oldScreen.close(screen)) return false;
        } else if (!force && oldScreen != null && (oldScreen.close(screen) || ScreenEvents.CLOSE.factory().onClose(oldScreen).isCanceled())) {
            return false;
        } else if (force && oldScreen != null) {
            ScreenEvents.FORCE_CLOSE.factory().onForceClose(this.screen);
        }
        var newScreen = screen;
        if (newScreen == null && this.isInMainMenus())
            newScreen = new TitleScreen();

        if (force) {
            ScreenEvents.FORCE_OPEN.factory().onOpen(newScreen);
        } else {
            var result = ScreenEvents.OPEN.factory().onOpen(newScreen);
            newScreen = result.isInterrupted() ? result.getValue() : newScreen;
            if (result.isCanceled()) return false;
        }

        if (newScreen == null && this.isInMainMenus())
            newScreen = new TitleScreen();

//        this.getGameWindow().setCursor(newScreen != null ? newScreen.getDefaultCursor() : this.getDefaultCursor());

        if (newScreen != null) {
            Gdx.graphics.setCursor(this.arrowCursor);
            ScreenEvents.INIT.factory().onInit(newScreen);
            newScreen.init(this.getWidth(), this.getHeight());
            BubbleBlaster.newActivity(GameActivity.MENUS);
        } else {
            BubbleBlaster.newActivity(GameActivity.PLAYING);

            if (BubbleBlasterConfig.DEBUG_LOG_SCREENS.get())
                LOGGER.debug("Showing <<NO-SCENE>>");
        }
        this.screen = newScreen;
        return true;
    }

    @Nullable
    public Screen getCurrentScreen() {
        return this.screen;
    }

    /**
     * Load the world.
     *
     * @deprecated use the one with the seed instead.
     */
    @Deprecated
    @SuppressWarnings({"ConstantConditions", "PointlessBooleanExpression"})
    public void createGame() {
        this.createGame(512L);
    }

    /**
     * Load the world.
     *
     * @param seed generator seed
     */
    @SuppressWarnings({"ConstantConditions", "PointlessBooleanExpression"})
    public void createGame(long seed) {
        this.createGame(seed, Gamemodes.NORMAL.get());
    }

    /**
     * Create a new saved game.
     *
     * @param seed     generator seed.
     * @param gamemode game mode to use.
     */
    @SuppressWarnings({"ConstantConditions", "PointlessBooleanExpression"})
    public void createGame(long seed, Gamemode gamemode) {
        this.createGame(seed, gamemode, Difficulty.NORMAL);
    }

    /**
     * Create a new saved game.
     *
     * @param seed     generator seed.
     * @param gamemode game mode to use.
     */
    @SuppressWarnings({"ConstantConditions", "PointlessBooleanExpression"})
    public void createGame(long seed, Gamemode gamemode, Difficulty difficulty) {
        this.startGame(seed, gamemode, difficulty, GameSave.fromFile(GameFolders.SAVES_DIR.child("save")), true);
    }

    /**
     * Loads the default saved game.
     */
    @SuppressWarnings({"ConstantConditions", "PointlessBooleanExpression"})
    public void loadGame() throws IOException {
        this.loadGame(GameSave.fromFile(GameFolders.SAVES_DIR.child("save")));
    }

    /**
     * Load a saved game.
     *
     * @param save the game save to load.
     */
    @SuppressWarnings({"ConstantConditions", "PointlessBooleanExpression"})
    public void loadGame(GameSave save) throws IOException {
        var seed = save.getSeed();
        var gamemode = save.getGamemode();
        var difficulty = save.getDifficulty();
        this.startGame(seed, gamemode, difficulty, save, false);
    }

    /**
     * Start the world.
     */
    @SuppressWarnings({"ConstantConditions", "PointlessBooleanExpression"})
    public void startGame(long seed, Gamemode gamemode, Difficulty difficulty, GameSave save, boolean create) {
        // Start loading.
        var screen = new MessengerScreen();

        // Show world loader screen.
        this.showScreen(screen);
        try {
            var saveHandle = save.getHandle();
            if (create && saveHandle.exists()) {
                if (saveHandle.isDirectory()) saveHandle.deleteDirectory();
                else saveHandle.delete();
            }

            if (!saveHandle.exists()) {
                saveHandle.mkdirs();
            }

            var world = new World(save, gamemode, difficulty, seed);

            if (create) {
                screen.setDescription("Preparing creation");
                world.prepareCreation(save);
            } else {
                screen.setDescription("Loading data...");
                gamemode = Gamemode.loadState(save, screen.getMessenger());
            }

            if (create) {
                screen.setDescription("Initializing world");
                world.firstInit(screen.getMessenger());
                screen.setDescription("Saving initialized save");
                world.save(save, screen.getMessenger());
            } else {
                world.load(save, screen.getMessenger());
            }

            var loadedGame = new LoadedGame(save, world);
            loadedGame.start();

            this.world = world;
            this.loadedGame = loadedGame;
        } catch (Throwable t) {
            LOGGER.error("Bubble Blaster failed to launch", t);
            var crashLog = new CrashLog("Game save being loaded", t);
            crashLog.add("Save Directory", save.getHandle());
            crashLog.add("Current Description", screen.getDescription());
            crashLog.add("Create Flag", create);
            crashLog.add("Seed", seed);
            crashLog.add("Gamemode", Registries.GAMEMODES.getKey(gamemode));
            BubbleBlaster.crash(crashLog.createCrash());
            return;
        }

        this.menuMusic.stop();

        BubbleBlaster.getInstance().showScreen(null);

        this.gameplayMusic.play();
    }

    /**
     * Quit world and loaded game.
     */
    public void quitLoadedGame() {
        var loadedGame = this.getLoadedGame();
        if (loadedGame != null) {
            loadedGame.end();
        }

        this.loadedGame = null;

        this.showScreen(new TitleScreen());
    }

    /**
     * Checks if the current thread is the main thread.
     *
     * @return true if the method is called on the main thread.
     */
    public static boolean isOnTickingThread() {
        return Thread.currentThread() == tickingThread;
    }

    /**
     * Checks if the current thread is the rendering thread.
     *
     * @return true if the method is called on the rendering thread.
     */
    public static boolean isOnRenderingThread() {
        return Thread.currentThread().getId() == renderingThread.getId();
    }

    /**
     * Checks if the world is active.
     * Note that this will return true even if the game is paused.
     *
     * @return true if world is active, even if game is paused.
     */
    public boolean isInGame() {
        return this.getLoadedGame() != null && this.world != null;
    }

    /**
     * Checks if the world isn't running. Aka, is in menus like the title screen.
     *
     * @return true if in main menus.
     */
    public boolean isInMainMenus() {
        return this.getLoadedGame() == null && this.world == null;
    }

    @NotNull
    public ResourceManager getResourceManager() {
        return this.resourceManager;
    }

    public TextureManager getTextureManager() {
        return this.textureManager;
    }

    /////////////////////////
    //     Loaded Game     //
    /////////////////////////
    public @Nullable LoadedGame getLoadedGame() {
        return this.loadedGame;
    }

    public @Nullable World getWorld() {
        return this.world;
    }

    public @Nullable GameSave getCurrentSave() {
        var loadedGame = this.getLoadedGame();
        if (loadedGame != null) {
            return loadedGame.getGameSave();
        }
        return null;
    }

    ///////////////////
    //     Fonts     //
    ///////////////////
    public FontSet getSansFont() {
        return this.sansFont;
    }

    public BitmapFont getMonospaceFont() {
        return this.monospaceFont;
    }

    public BitmapFont getPixelFont() {
        return this.pixelFont;
    }

    public BitmapFont getLogoFont() {
        return this.logoFont;
    }

    public Cursor.SystemCursor getTextCursor() {
        return Cursor.SystemCursor.Ibeam;
    }

    public Cursor.SystemCursor getPointerCursor() {
        return Cursor.SystemCursor.Arrow;
    }

    public Cursor.SystemCursor getDefaultCursor() {
        return Cursor.SystemCursor.Arrow;
    }

    public String getFontName() {
        return this.fontName;
    }

    ///////////////////////
    //     Rendering     //
    ///////////////////////
    @NotNull
    public RenderSettings getRenderSettings() {
        return this.renderSettings;
    }

    public WorldRenderer getWorldRenderer() {
        return this.worldRenderer;
    }

    ////////////////////
    //     Bounds     //
    ////////////////////
    public Rectangle getGameBounds() {
        return new Rectangle(0, 0, this.getWidth(), this.getHeight());
    }

    ////////////////////////
    //     Game flags     //
    ////////////////////////
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Loads the game.
     *
     * @param ignoredMainProgress main loading progress.
     */
    private void load(ProgressMessenger ignoredMainProgress) {

    }

    /**
     * Prepares the game.
     */
    private void prepare() {

    }

    /**
     * Prepares the player.
     */
    private void preparePlayer(Player ignoredPlayer) {

    }

    /**
     * Active game glitch.
     */
    public void glitch() {
        this.isGlitched = true;
    }

    /**
     * @return the amount create main loading steps.
     */
    private int getMainLoadingSteps() {
        return 0;
    }

    @SuppressWarnings("DuplicatedCode")
    private void ticking() {
        var tickCap = 1000.0 / (double) TPS;
        var tickTime = 0d;
        var gameFrameTime = 0d;
        var ticksPassed = 0;

        double time = System.currentTimeMillis();

        this.initialGameTick();

        try {
            while (this.running) {
                try {
                    var canTick = false;

                    double time2 = System.currentTimeMillis();
                    var passed = time2 - time;
                    gameFrameTime += passed;
                    tickTime += passed;

                    time = time2;

                    while (gameFrameTime >= tickCap) {
                        gameFrameTime -= tickCap;

                        canTick = true;
                    }

                    if (canTick) {
                        ticksPassed++;
                        try {
                            this.gameTick();
                        } catch (Throwable t) {
                            var crashLog = new CrashLog("Game being ticked.", t);
                            BubbleBlaster.crash(crashLog.createCrash());
                        }
                    }

                    this.lastTickMs = System.currentTimeMillis();

                    if (tickTime >= 1000.0d) {
                        this.currentTps = ticksPassed;
                        ticksPassed = 0;
                        tickTime = 0;
                    }
                } catch (OutOfMemoryError error) {
                    if (this.outOfMemory(error)) return;
                }
            }
        } catch (Throwable t) {
            var crashLog = new CrashLog("Running game loop.", t);
            BubbleBlaster.crash(crashLog.createCrash());
        }
    }

    private void gameTick() {
        tickExecutor.pollAll();

        if (this.isGlitched && BubbleBlasterConfig.ENABLE_ANNOYING_EASTER_EGGS.get()) {
            this.glitchRenderer.tick();
            return;
        }

        // Tick input
        var screen = this.screen;
        if (screen != null) {
            this.controllerInput.tickScreen(screen);
            this.desktopInput.tickScreen(screen);
            this.mobileInput.tickScreen(screen);
        }
        final var player = this.player;
        if (player != null && screen == null) {
            this.controllerInput.tickPlayer(player);
            this.desktopInput.tickPlayer(player);
            this.mobileInput.tickPlayer(player);
        }

        if (this.isLoaded()) {
            TickEvents.PRE_TICK_GAME.factory().onTickGame(this);
        }

        final var world = this.world;
        final var loadedGame = this.loadedGame;
        if (world != null && loadedGame != null && !BubbleBlaster.isPaused()) {
            this.controllerInput.tickWorld(world, loadedGame);
            this.desktopInput.tickWorld(world, loadedGame);
            this.mobileInput.tickWorld(world, loadedGame);

            final var gamemode = world.getGamemode();
            if (gamemode != null && player != null && player.getLevel() > 255)
                BubbleBlaster.getInstance().glitch();

            TickEvents.PRE_TICK_WORLD.factory().onTickWorld(world);
            world.tick();
            TickEvents.POST_TICK_WORLD.factory().onTickWorld(world);
            TickEvents.PRE_TICK_LOADED_GAME.factory().onTickLoadedGame(loadedGame);
            loadedGame.tick();
            TickEvents.POST_TICK_LOADED_GAME.factory().onTickLoadedGame(loadedGame);
        }

        if (screen != null)
            screen.tick();

        if (this.ticker.advance() == 40) {
            this.ticker.reset();
            if (this.isLoaded())
                this.tickRichPresence(player);
        }

        BubbleBlaster.ticks++;

        // Call tick event.
        if (this.isLoaded()) {
            TickEvents.POST_TICK_GAME.factory().onTickGame(this);
        }
    }

    private void tickRichPresence(@Nullable Player player) {

    }

    /**
     * @param renderer the renderer to render the game with.
     * @param mouseX   current mouse X position.
     * @param mouseY   current mouse Y position.
     */
    private void renderGame(Renderer renderer, int mouseX, int mouseY, float frameTime) {
        // Call to world rendering.
        // Get field and set local variable. For multithreaded null-safety.
        @Nullable Screen screen = this.getCurrentScreen();
        @Nullable World world = this.world;
        @Nullable WorldRenderer worldRenderer = this.worldRenderer;

        // Post event before rendering the game.
        RenderEvents.RENDER_GAME_BEFORE.factory().onRenderGameBefore(this, renderer, frameTime);

        // Handle music
        if (!this.gameplayMusic.isPaused()) {
            this.gameplayMusic.update(Gdx.graphics.getDeltaTime());
        } else if (!this.menuMusic.isPaused()) {
            this.menuMusic.update(Gdx.graphics.getDeltaTime());
        }

        // Render world.
        this.profiler.section("Render World", () -> {
            if (world != null && worldRenderer != null) {
                if (screen != null) {
                    renderer.blurred(true, () -> {
                        RenderEvents.RENDER_WORLD_BEFORE.factory().onRenderWorldBefore(world, worldRenderer, renderer);
                        worldRenderer.render(renderer, mouseX, mouseY, frameTime);
                        RenderEvents.RENDER_WORLD_AFTER.factory().onRenderWorldAfter(world, worldRenderer, renderer);
                    });
                } else {
                    RenderEvents.RENDER_WORLD_BEFORE.factory().onRenderWorldBefore(world, worldRenderer, renderer);
                    worldRenderer.render(renderer, mouseX, mouseY, frameTime);
                    RenderEvents.RENDER_WORLD_AFTER.factory().onRenderWorldAfter(world, worldRenderer, renderer);
                }
            }
        });

        // Render screen.
        this.profiler.section("Render Screen", () -> {
            if (screen != null) {
                RenderEvents.RENDER_SCREEN_BEFORE.factory().onRenderScreenBefore(screen, renderer);
                screen.render(this, renderer, mouseX, mouseY, frameTime);
                if (GamePlatform.get().isMobile()) {
                    screen.renderCloseButton(renderer, mouseX, mouseY);
                }
                RenderEvents.RENDER_SCREEN_AFTER.factory().onRenderScreenAfter(screen, renderer);
            }
            if (world != null && worldRenderer != null) {
                renderer.fillEffect(0, 0, BubbleBlaster.getInstance().getWidth(), 3);
            }
        });

        // Post render.
        this.profiler.section("Post Render", () -> this.postRender(renderer));

        // Post event after rendering the game.
        RenderEvents.RENDER_GAME_AFTER.factory().onRenderGameAfter(this, renderer, frameTime);
    }

    private void postRender(Renderer renderer) {
        if (this.fadeIn) {
            final var timeDiff = System.currentTimeMillis() - this.fadeInStart;
            if (timeDiff <= this.fadeInDuration) {
                var clamp = (int) Mth.clamp(255 * (1f - (float) timeDiff / this.fadeInDuration), 0, 255);
                var color = Colors.rgba(0, 0, 0, clamp);
                renderer.fill(0, 0, this.getWidth(), this.getHeight(), color);
            } else {
                this.fadeIn = false;
            }
        }
    }

    /**
     * Starts loading the game.
     */
    public void startLoading() {
        this.getGameWindow().init();
    }

    /**
     * @return get the default font.
     */
    public BitmapFont getFont() {
        return this.getSansFont();
    }

    public boolean hasScreenOpen() {
        return this.screen != null;
    }

    private void log(String text) {
        // Todo: implement
    }

    /**
     * This method should be overridden when filters are used in the game.<br>
     * Should be overridden for enabling / disabling render filters.
     *
     * @return list create buffered image operations.
     */
    @NotNull
    public List<VfxEffect> getCurrentFilters() {
        return new ArrayList<>();
    }

    public Player loadPlayerIntoWorld(@NotNull World world) {
        var player = new Player(world);

        this.input = player;
        this.player = player;

        this.preparePlayer(player);
        return player;
    }

    /////////////////////
    //     Getters     //
    /////////////////////
    public float getGameFrameTime() {
        return Gdx.graphics.getDeltaTime();
    }

    public boolean isAntialiasEnabled() {
        return true;
    }

    public boolean isTextAntialiasEnabled() {
        return this.isAntialiasEnabled();
    }

    public GameWindow getGameWindow() {
        return this.window;
    }

    public Rectangle getBounds() {
        return new Rectangle(0, 0, this.window.getWidth(), this.window.getHeight());
    }

    public int getWidth() {
        return Constants.DEFAULT_SIZE.x;
    }

    public int getHeight() {
        return Constants.DEFAULT_SIZE.y;
    }

    /** Convert a window-space pointer position to the fixed logical game area. */
    public Vector2 windowToGame(Vector2 position) {
        return this.window.windowToGame(position);
    }

    public int getFps() {
        return this.fps;
    }

    public int getCurrentTps() {
        return this.currentTps;
    }

    public Instant getLastTickTime() {
        return Instant.ofEpochMilli(this.lastTickMs);
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public boolean isLoading() {
        return !this.loaded;
    }

    public boolean isStopping() {
        return this.stopping;
    }

    /**
     * Clean shutdown create the game. By setting the running flag to false, and stop flag to true.
     */
    public void shutdown() {
        if (GamePlatform.get().isMobile()) {
            GamePlatform.get().toggleDebugGui();
            return;
        }
        this.stopping = true;
        this.running = false;
        Gdx.app.exit();
    }

    public static void crash(ApplicationCrash crash) {
        try {
            crash.printCrash();

            var crashLog = crash.getCrashLog();
            GamePlatform.get().handleCrash(crashLog);
            if (GamePlatform.get().isDesktop()) instance.shutdown();
        } catch (Throwable t) {
            LOGGER.error(FATAL_ERROR_MSG, t);
            instance.shutdown();
        }
    }

    @EnsuresNonNull({"ticking"})
    @SuppressWarnings("Convert2Lambda")
    public void windowLoaded() {
        this.running = true;

        tickingThread = new Thread(BubbleBlaster.this::ticking, "Ticker");
        tickingThread.start();
        tickExecutor = new PollingExecutorService(tickingThread);

        BubbleBlaster.getLogger().info("Game threads started!");
    }

    public static void crash(Throwable t) {
        var crashLog = new CrashLog("Unknown source", t);
        BubbleBlaster.crash(crashLog.createCrash());
    }

    public void finalizeSetup() {

    }

    public void fadeIn(float time) {
        this.fadeIn = true;
        this.fadeInStart = System.currentTimeMillis();
        this.fadeInDuration = time;
    }

    public void finish() {
        this.glitchRenderer = new GlitchRenderer(this);
        this.showScreen(new TitleScreen());
        this.loaded = true;

        this.afterLoading.values().forEach(Runnable::run);
        this.afterLoading.clear();

        BubbleBlaster.newActivity(GameActivity.MENUS);

        LifecycleEvents.FINISHED.factory().onFinished(this);
    }

    public long serializeSeed(String text) {
        var seedNr = Long.getLong(text);
        if (seedNr != null) {
            return seedNr;
        }

        long h = 0;
        for (var c : text.toCharArray()) {
            h = 31L * h + c;
        }
        return h;
    }

    public void saveAndQuit() {
        if (this.world == null)
            throw new IllegalStateException("World isn't loaded.");

        this.world.save();
        this.quitLoadedGame();
    }

    public GlobalSaveData getGlobalData() {
        return this.globalSaveData;
    }

    public BitmapFont getBitmapFont() {
        return new BitmapFont();
    }

    public boolean isFocused() {
        return this.window.isFocused();
    }

    public Renderer getRenderer() {
        return this.currentRenderer;
    }

    public SpriteBatch getBatch() {
        return this.batch;
    }

    @SuppressWarnings("RedundantIfStatement")
    @ApiStatus.Internal
    @CanIgnoreReturnValue
    public boolean keyPress(int keycode) {
        var screen = this.getCurrentScreen();
        var world = this.world;

        if (keycode == Keys.F12) {
            GamePlatform.get().toggleDebugGui();
            return true;
        }

        if (keycode == Keys.F11 && Flags.ALLOW_FULLSCREEN && this.window.toggleFullscreen())
            return true;

        if (keycode == Keys.F2 && Screenshot.take() != null)
            return true;

        if (screen != null)
            return screen.keyPress(keycode);

        if (world != null && this.keyPressEnv(world, keycode))
            return true;

        if (keycode == Keys.ESCAPE && world != null && world.isAlive() && BubbleBlaster.getInstance().showScreen(new PauseScreen()))
            return true;

        return false;
    }

    @ApiStatus.Internal
    @CanIgnoreReturnValue
    private boolean keyPressEnv(World world, int keycode) {
        var isDev = this.canExecuteDevCommands();

        if (isDev && keycode == Keys.F10 && world.beginEvent(GameplayEvents.BLOOD_MOON_EVENT))
            return true;

        return keycode == Keys.SLASH && !this.hasScreenOpen() && BubbleBlaster.getInstance().showScreen(new CommandScreen());
    }

    private boolean canExecuteDevCommands() {
        return BubbleBlaster.isDebugMode() || GamePlatform.get().isDevelopmentEnvironment();
    }

    @ApiStatus.Internal
    @CanIgnoreReturnValue
    public boolean keyRelease(int keycode) {
        @Nullable Screen screen = this.getCurrentScreen();
        return screen != null && screen.keyRelease(keycode);

    }

    @ApiStatus.Internal
    @CanIgnoreReturnValue
    public boolean charType(char character) {
        @Nullable Screen screen = this.getCurrentScreen();
        return screen != null && screen.charType(character);

    }

    @ApiStatus.Internal
    @CanIgnoreReturnValue
    public boolean mousePress(int screenX, int screenY, int ignoredPointer, int button) {
        @Nullable Screen screen = this.getCurrentScreen();
        return screen != null && screen.mousePress(screenX, screenY, button);

    }

    @ApiStatus.Internal
    @CanIgnoreReturnValue
    public boolean mouseRelease(int screenX, int screenY, int ignoredPointer, int button) {
        @Nullable Screen screen = this.getCurrentScreen();
        return screen != null && screen.mouseRelease(screenX, screenY, button);

    }

    @ApiStatus.Internal
    @CanIgnoreReturnValue
    public boolean mouseDragged(int oldX, int oldY, int newX, int newY, int ignoredPointer, int button) {
        var screen = this.getCurrentScreen();
        if (screen != null) screen.mouseDrag(oldX, oldY, newX, newY, button);

        return true;
    }

    @ApiStatus.Internal
    @CanIgnoreReturnValue
    public boolean mouseMove(int screenX, int screenY) {
        @Nullable Screen screen = this.getCurrentScreen();
        if (screen != null) screen.mouseMove(screenX, screenY);

        return true;
    }

    @ApiStatus.Internal
    @CanIgnoreReturnValue
    public boolean mouseWheel(int x, int y, float ignoredAmountX, float amountY) {
        var screen = this.getCurrentScreen();
        return screen != null && screen.mouseWheel(x, y, amountY);

    }

    @Override
    public void fillInCrash(CrashLog crashLog) {
        var world = this.world;
        if (world != null) {
            world.fillInCrash(crashLog);
        }

        var screen = this.getCurrentScreen();
        if (screen != null) {
            screen.fillInCrash(crashLog);
        }
    }

    public boolean isCollisionShapesShown() {
        return GamePlatform.get().isCollisionShapesShown();
    }

    public InputType getCurrentInput() {
        return this.currentInput;
    }

    public void setCurrentInput(InputType currentInput) {
        this.currentInput = currentInput;
    }

    public Matrix4 getTransform() {
        return this.shapes.getTransformMatrix();
    }

    public void addXp(double experience) {
        if (!Double.isFinite(experience) || experience <= 0) return;

        this.xp = this.globalSaveData.getXp();
        if (!Double.isFinite(this.xp) || this.xp < 0) this.xp = 0;

        this.xpLevel = Math.max(0, this.globalSaveData.getXpLevel());
        this.money = this.globalSaveData.getMoney();
        this.xp += experience * Math.PI;
        this.money += experience;

        while (this.xpLevel < Integer.MAX_VALUE && this.xp >= this.getXpForLevelUp(this.xpLevel)) {
            this.levelUpXp();
        }

        this.globalSaveData.setXp(this.xp);
        this.globalSaveData.setXpLevel(this.xpLevel);
        this.globalSaveData.setMoney(this.money);

        try {
            this.globalSaveData.dump();
        } catch (IOException e) {
            BubbleBlaster.LOGGER.error("Failed to save money:", e);
            BubbleBlaster.getInstance().notifications.notify(Notification.builder("Error!", "Saving money failed!").subText("Global Save Data").build());
        }
    }

    public double getXpForLevelUp(int xpLevel) {
        double level = Math.max(0, xpLevel);
        return 3000.0 * Math.pow(level + 1.0, 1.5);
    }

    private void levelUpXp() {
        this.xp -= this.getXpForLevelUp(this.xpLevel);
        this.xpLevel++;
    }
}
