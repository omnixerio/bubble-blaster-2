package dev.ultreon.bubbles.render.gui.screen;

import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.GamePlatform;
import dev.ultreon.bubbles.init.Fonts;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.TitleScreenBackground;
import dev.ultreon.bubbles.render.gui.screen.options.LanguageScreen;
import dev.ultreon.bubbles.render.gui.screen.options.OptionsScreen;
import dev.ultreon.bubbles.render.gui.widget.Button;
import dev.ultreon.bubbles.text.TextObject;

public class TitleScreen extends Screen {

    public TitleScreen() {

    }

    private void openSavesSelection() {
        this.game.showScreen(new SavesScreen(this));
    }

    private void openModList() {
        var screen = GamePlatform.get().openModListScreen();
        if (screen == null) {
            this.game.notifications.unavailable("Mods on " + GamePlatform.get().getOperatingSystem().toString());
            return;
        }
        this.game.showScreen(screen);
    }

    private void openLanguageSettings() {
        this.game.showScreen(new LanguageScreen());
    }

    private void openOptions() {
        this.game.showScreen(new OptionsScreen());
    }

    private void startGame() {
        this.game.showScreen(new NewGameScreen());
    }

    @Override
    public void init() {
        this.clearWidgets();

        if (this.game.menuMusic.isStopped() || this.game.menuMusic.isPaused()) {
            this.game.menuMusic.play();
        }

        this.add(Button.builder()
                .bounds(this.width / 2 - 200, 220, 400, 60)
                .text(TextObject.translation("bubbleblaster.screen.title.start"))
                .command(this::startGame)
                .font(Fonts.SANS_BIG.get())
                .build());
        this.add(Button.builder()
                .bounds(this.width / 2 - 200, 300, 400, 60)
                .text(TextObject.translation("bubbleblaster.screen.title.shop"))
                .command(this::openShop)
                .font(Fonts.SANS_BIG.get())
                .build());
        this.add(Button.builder()
                .bounds(this.width / 2 - 200, 380, 190, 60)
                .text(TextObject.translation("bubbleblaster.screen.title.mods"))
                .command(this::openModList)
                .font(Fonts.SANS_BIG.get())
                .build());
        this.add(Button.builder()
                .bounds(this.width / 2 + 10, 380, 190, 60)
                .text(TextObject.translation("bubbleblaster.screen.title.options"))
                .command(this::openOptions)
                .font(Fonts.SANS_BIG.get())
                .build());
        this.add(Button.builder()
                .bounds(this.width / 2 - 200, 460, 190, 60)
                .text(TextObject.translation("bubbleblaster.screen.title.language"))
                .command(this::openLanguageSettings)
                .font(Fonts.SANS_BIG.get())
                .build());
        this.add(Button.builder()
                .bounds(this.width / 2 + 10, 460, 190, 60)
                .text(TextObject.translation("bubbleblaster.screen.title.quit"))
                .command(this.game::shutdown)
                .font(Fonts.SANS_BIG.get())
                .build());
        this.add(Button.builder()
                .bounds(this.width - 40 - 180, 100, 180, 40)
                .text(TextObject.translation("bubbleblaster.screen.title.resetData"))
                .command(this::resetData)
                .font(Fonts.SANS_BIG.get())
                .build());
    }

    private void openShop() {
        this.game.showScreen(new ShopScreen());
    }

    private void resetData() {
        this.game.showScreen(new ConfirmScreen(() -> {
            var globalData = this.game.getGlobalData();
            globalData.reset();
        }));
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(BubbleBlaster game, Renderer renderer, int mouseX, int mouseY, float deltaTime) {
        renderer.fill(0, 0, this.width, 175, Colors.grayscale(0x1e));

        renderer.fillEffect(0, 175, this.width, 3);
        renderer.fillGradient(0, 178, this.width, 20, Colors.argb(0x20000000), Colors.TRANSPARENT);

        renderer.drawTextCenter(Fonts.DONGLE_TITLE.get(), "Bubble Blaster", this.width / 2f, 87.5f, Colors.WHITE);

        renderer.drawText(this.monospaced, "Game Version: " + BubbleBlaster.getGameVersion(), 40, 40, Colors.WHITE);
        renderer.drawText(this.monospaced, "LibGDX Version: " + BubbleBlaster.getLibGDXVersion(), 40, 52, Colors.WHITE);

        if (GamePlatform.get().allowsMods()) {
            renderer.drawText(this.monospaced, "Loader Version: " + BubbleBlaster.getFabricLoaderVersion(), 40, 64, Colors.WHITE);
            renderer.drawText(this.monospaced, "Mods Loaded: " + GamePlatform.get().getModsCount(), 40, 76, Colors.WHITE);
        }

        var highScore = game.getGlobalData().getHighScore();
        var money = game.getGlobalData().getMoney();
        var xp = game.getGlobalData().getXp();
        var xpLevel = game.getGlobalData().getXpLevel();
        var xpForLevelUp = game.getXpForLevelUp(xpLevel);
//        renderer.drawTextRight(Fonts.SANS_PARAGRAPH.get(), "Highscore: " + Math.round(highScore), this.width - 40, 40, Colors.WHITE);
//        renderer.drawTextRight(Fonts.SANS_PARAGRAPH.get(), "Money: " + Math.round(money), this.width - 40, 60, Colors.WHITE);
//        renderer.drawTextRight(Fonts.SANS_PARAGRAPH.get(), "XP: " + Math.round(xp) + " / " + Math.round(xpForLevelUp) + " (lvl. " + xpLevel + ")", this.width - 40, 80, Colors.WHITE);
        renderer.drawTextRight(Fonts.SANS_PARAGRAPH.get(), TextObject.translation("bubbleblaster.screen.title.highscore", Math.round(highScore)), this.width - 40, 40, Colors.WHITE);
        renderer.drawTextRight(Fonts.SANS_PARAGRAPH.get(), TextObject.translation("bubbleblaster.screen.title.money", money), this.width - 40, 60, Colors.WHITE);
        renderer.drawTextRight(Fonts.SANS_PARAGRAPH.get(), TextObject.translation("bubbleblaster.screen.title.xp", Math.round(xp), Math.round(xpForLevelUp), xpLevel), this.width - 40, 80, Colors.WHITE);

        this.renderChildren(renderer, mouseX, mouseY, deltaTime);
    }

    @Override
    public void renderCloseButton(Renderer renderer, int mouseX, int mouseY) {

    }

    @Override
    public boolean mousePress(int x, int y, int button) {
        return super.mousePress(x, y, button);
    }

    @Override
    public boolean close(Screen to) {
        if (to instanceof TitleScreen) return true;
        return super.close(to);
    }

    @Override
    public boolean keyPress(int keyCode) {
        return false;
    }
}
