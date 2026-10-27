package dev.ultreon.bubbles.render.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.BubbleBlasterConfig;
import dev.ultreon.bubbles.GamePlatform;
import dev.ultreon.bubbles.command.*;
import dev.ultreon.bubbles.data.GlobalSaveData;
import dev.ultreon.bubbles.entity.bubble.BubbleSystem;
import dev.ultreon.bubbles.event.v1.ConfigEvents;
import dev.ultreon.bubbles.event.v1.GameEvents;
import dev.ultreon.bubbles.event.v1.LifecycleEvents;
import dev.ultreon.bubbles.init.HudTypes;
import dev.ultreon.bubbles.registry.RegisterHandler;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.hud.HudType;
import dev.ultreon.bubbles.settings.GameSettings;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.libs.commons.v0.Messenger;
import dev.ultreon.libs.commons.v0.MessengerImpl;
import dev.ultreon.libs.commons.v0.ProgressMessenger;
import dev.ultreon.libs.crash.v0.CrashLog;
import dev.ultreon.libs.registries.v0.Registry;
import dev.ultreon.libs.registries.v0.event.RegistryEvents;
import dev.ultreon.bubbles.lang.LanguageManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class LoadScreen extends InternalScreen {
    public static final Color BACKGROUND = Colors.rgb(0x484848);
    private static final Color TEXT_COLOR = Colors.rgb(0xc0c0c0);
    public static final Color PROGRESSBAR_BG = Colors.rgb(0x808080);
    public static final float PROGRESS_BAR_WIDTH = 500f;

    private static final Logger LOGGER = GamePlatform.get().getLogger("Game-Loader");
    private static LoadScreen instance = null;
    private static volatile boolean languagesLoaded = false;
    private static boolean done;
    private volatile ProgressMessenger progressMain = null;
    private volatile ProgressMessenger progressAlt = null;
    private final Messenger msgMain = new MessengerImpl(this::logMain);
    private final Messenger msgAlt = new MessengerImpl(this::logAlt);
    private String curMainMsg = "";
    private String curAltMsg = "";
    private long startTime;
    private static Thread thread;

    public LoadScreen() {
        instance = this;
    }

    public static LoadScreen get() {
        return done ? null : instance;
    }

    public static boolean isLanguagesLoaded() {
        return languagesLoaded;
    }

    @Override
    public void init() {
        if (thread == null) {
            thread = new Thread(this::doLoading, "Loading-Thread");
            thread.start();
        }
    }

    @Override
    public boolean close(Screen to) {
        var done = LoadScreen.isDone();
        if (done) {
            return super.close(to);
        }
        return true;
    }

    @Override
    public void render(BubbleBlaster game, Renderer renderer, int mouseX, int mouseY, float deltaTime) {
        if (this.startTime == 0L) {
            this.startTime = System.currentTimeMillis();
        }

        this.renderBackground(renderer);

        // Draw progress components.
        if (this.progressMain != null) {
            var progress = this.progressMain.getProgress();
            var max = this.progressMain.getMax();

            renderer.setLineThickness(9.0f);

            // Draw current 1st line message.
            if (this.curMainMsg != null) {
                renderer.drawTextCenter(this.font, this.curMainMsg, this.width / 2f, this.height / 2f, TEXT_COLOR);
            }

            renderer.fill(this.width / 2f - PROGRESS_BAR_WIDTH / 2, this.height / 2 + 19 + 2, PROGRESS_BAR_WIDTH, 1, PROGRESSBAR_BG);

            renderer.setColor(Colors.rgb(0x0040ff));
            var effectWidth = (int) (PROGRESS_BAR_WIDTH * (double) (progress + 1) / (double) max);
            if (effectWidth >= 1)
                renderer.fillEffect(this.width / 2f - PROGRESS_BAR_WIDTH / 2, (float) this.height / 2 + 19, effectWidth, 5);

            // Draw 2nd progress components.
            if (this.progressAlt != null) {
                var progressSub = this.progressAlt.getProgress();
                var maxSub = this.progressAlt.getMax();

                // Draw current 2nd line message.
                if (this.curAltMsg != null) {
                    renderer.drawTextCenter(this.font, this.curAltMsg, this.width / 2f, this.height / 2f + 75, TEXT_COLOR);
                }

                renderer.fill(this.width / 2f - PROGRESS_BAR_WIDTH / 2, this.height / 2 + 94 + 2, PROGRESS_BAR_WIDTH, 1, PROGRESSBAR_BG);

                var effectWidthSub = (int) (PROGRESS_BAR_WIDTH * (double) (progressSub + 1) / (double) maxSub);
                if (effectWidthSub >= 1)
                    renderer.fillEffect(this.width / 2f - PROGRESS_BAR_WIDTH / 2, (float) this.height / 2 + 94, effectWidthSub, 5);
            }
        }
    }

    private void doLoading() {
        try {
            this.loadGame();
        } catch (Throwable throwable) {
            LOGGER.error("Game loading failed", throwable);
            LoadScreen.done = true;
            BubbleBlaster.invoke(() -> this.game.showScreen(new CrashScreen(new CrashLog("Game loading failed", throwable)), true));
        }
    }

    private void loadGame() {
        LOGGER.info("Loading started");

        GameSettings.nopInit();
        BubbleBlasterConfig.register();

        LifecycleEvents.LOADING.factory().onLoading(this.game, this);

        this.progressMain = new ProgressMessenger(this.msgMain, GamePlatform.get().allowsMods() ? 10 : 9);

        // Get game directory in Java's File format.
        var dataDir = GamePlatform.get().getDataDirectory();

        // Check game directory exists, if not, create it!
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        LOGGER.info("Loading resources...");
        this.progressMain.sendNext("Loading resources...");
        var progressAltAtomic = new AtomicReference<>(this.progressAlt);
        GamePlatform.get().loadGameResources(progressAltAtomic, this.msgAlt);
        GamePlatform.get().loadModResources(progressAltAtomic, this.msgAlt);
        GameEvents.RESOURCES_LOADED.factory().onResourcesLoaded(this.game().getResourceManager());
        this.progressAlt = progressAltAtomic.get();

        if (GamePlatform.get().allowsMods()) {
            LOGGER.info("Setting up mods...");
            this.progressMain.sendNext("Setting up mods...");
            GamePlatform.get().setupMods();
        }

        // Set up components in registry.
        this.progressMain.sendNext("Registering components...");
        this.registerComponents();

        // Loading object holders
        this.progressMain.sendNext("Loading the fonts...");
        this.loadFonts();

        this.progressMain.sendNext("Setting up miscellaneous stuff...");
        this.setupCommandsAndGlobalData();

        this.progressMain.sendNext("Registering languages...");
        this.registerLanguages();

        this.progressMain.sendNext("Setting up texture collections....");
        this.setupTextureCollections();

        // BubbleSystem
        this.progressMain.sendNext("Initialize bubble system...");
        BubbleSystem.init();

        // Load complete.
        this.progressMain.sendNext("Finalize setup...");
        this.game.finalizeSetup();

        Registry.dump();

        ConfigEvents.RELOAD_ALL.factory().onReloadAll();

        int fps = BubbleBlasterConfig.MAX_FRAMERATE.get();
        Gdx.graphics.setForegroundFPS(fps == 240 ? 0 : fps);

        try {
            var hudId = Identifier.tryParse(BubbleBlasterConfig.GAME_HUD.getOrDefault());
            if (hudId != null) {
                var hud = Registries.HUD.getValue(hudId);
                HudType.setCurrent(hud);
            } else {
                HudType.setCurrent(HudTypes.MODERN.get());
                var id = HudTypes.MODERN.id();
                BubbleBlasterConfig.GAME_HUD.set(id.toString());
                BubbleBlasterConfig.save();
            }
        } catch (RuntimeException ignored) {

        }

        LoadScreen.done = true;

        try {
            LifecycleEvents.LOADED.factory().onLoaded(this.game, this);
        } finally {
            BubbleBlaster.invoke(this.game::finish);
        }
    }

    private void setupTextureCollections() {
        var values = Registries.TEXTURE_COLLECTIONS.values();
        this.progressAlt = new ProgressMessenger(this.msgAlt, values.size());
        for (var collection : values) {
            GameEvents.COLLECT_TEXTURES.factory().onCollectTextures(collection);
            this.progressAlt.sendNext(String.valueOf(Registries.TEXTURE_COLLECTIONS.getKey(collection)));
        }
        this.progressAlt = null;
    }

    public void registerLanguages() {
        this.registerLanguage("en_us");
        this.registerLanguage("en_gb");
        this.registerLanguage("nl_be");
        this.registerLanguage("nl_nl");
        this.registerLanguage("fy_nl");
        this.registerLanguage("de_de");
        this.registerLanguage("it_it");
        this.registerLanguage("fr_fr");
        this.registerLanguage("es_es");
        this.registerLanguage("af_za");
        this.registerLanguage("uk_ua");
        this.registerLanguage("hi_in");
        this.registerLanguage("tr_tr");
        this.registerLanguage("ko_kp");
        this.registerLanguage("ko_kr");
        this.registerLanguage("ru_ru");
        this.registerLanguage("zh_cn");
        this.registerLanguage("el_gr");
        this.registerLanguage("ja_jp");
        this.registerLanguage("vi_vn");

        LifecycleEvents.REGISTER_LANGUAGES.factory().onRegisterLanguages(this.game, this, this::registerLanguage);

        languagesLoaded = true;

        this.progressAlt = null;
    }

    private void registerLanguage(String code) {
        var s = code.split("_", 2);
        if (s.length == 0) throw new IllegalArgumentException("Language requires a non-empty string.");
        if (s.length == 1) throw new IllegalArgumentException("Language code needs to include country.");
        var locale = new Locale(s[0], s[1]);
        LanguageManager.INSTANCE.register(locale, BubbleBlaster.id(code));
        LanguageManager.INSTANCE.load(locale, BubbleBlaster.id(code), BubbleBlaster.getInstance().getResourceManager());
    }

    private void registerLanguage(Identifier code) {
        var s = code.path().split("_", 2);
        if (s.length == 0) throw new IllegalArgumentException("Language requires a non-empty string.");
        if (s.length == 1) throw new IllegalArgumentException("Language code needs to include country.");
        var locale = new Locale(s[0], s[1]);
        LanguageManager.INSTANCE.register(locale, code);
        LanguageManager.INSTANCE.load(locale, code, BubbleBlaster.getInstance().getResourceManager());
    }

    private void loadFonts() {
        BubbleBlaster.invokeAndWait(() -> this.game().loadFonts());
        this.progressAlt = null;
    }

    private void registerComponents() {
        var registries = Registry.getRegistries();
        this.progressAlt = new ProgressMessenger(this.msgAlt, registries.size());
        for (var registry : registries) {
            this.progressAlt.send(registry.id().toString());
            this.progressAlt.increment();
            RegistryEvents.AUTO_REGISTER.factory().onAutoRegister(registry);
            registry.entries().forEach(e -> {
                 if (e.getValue() instanceof RegisterHandler) {
                     var handler = (RegisterHandler) e.getValue();
                     handler.onRegister(e.getKey());
                 }
            });
        }
        Registry.freeze();
        this.progressAlt = null;
    }

    private void addModIcon(String modId, Identifier path) {
        GamePlatform.get().setCustomIcon(modId, path);
    }

    private BubbleBlaster game() {
        return this.game;
    }

    public static boolean isDone() {
        return LoadScreen.done;
    }

    public void setupCommandsAndGlobalData() {
        var main = this.game;

        // Commands
        CommandConstructor.add("tp", new TeleportCommand());
        CommandConstructor.add("heal", new HealCommand());
        CommandConstructor.add("level", new LevelCommand());
        CommandConstructor.add("health", new HealthCommand());
        CommandConstructor.add("max-health", new MaxHealthCommand());
        CommandConstructor.add("score", new ScoreCommand());
        CommandConstructor.add("effect", new EffectCommand());
        CommandConstructor.add("spawn", new SpawnCommand());
        CommandConstructor.add("teleport", new TeleportCommand());
        CommandConstructor.add("game-over", new GameOverCommand());
        CommandConstructor.add("blood-moon", new BloodMoonCommand());
        CommandConstructor.add("gameplay", new GameplayCommand());
        CommandConstructor.add("echo", new EchoCommand());

        try {
            GlobalSaveData.instance().load();
            GlobalSaveData.instance().dump();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.progressAlt = null;
    }

    private void logAlt(String s) {
        this.curAltMsg = s;
    }

    private void logMain(String s) {
        this.curMainMsg = s;
    }
}
