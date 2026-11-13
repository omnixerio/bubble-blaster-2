package dev.ultreon.bubbles.platform.android;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Rectangle;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.BubbleBlasterConfig;
import dev.ultreon.bubbles.Constants;
import dev.ultreon.bubbles.GameWindow;
import dev.ultreon.bubbles.event.v1.WindowEvents;
import dev.ultreon.bubbles.util.exceptions.OneTimeUseException;
import dev.ultreon.libs.commons.v0.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Objects;

/**
 * Window class for the game's window.
 *
 * @author XyperCode
 */
@SuppressWarnings("unused")
public class AndroidGameWindow implements GameWindow {
    private static final Marker MARKER = MarkerFactory.getMarker("GameWindow");

    private boolean initialized = false;

    
    private int fps;
    private Thread mainThread;
    private boolean visible;

    /**
     * Window constructor.
     */
    public AndroidGameWindow(Properties properties) {
        this.setSize(properties.width, properties.height);
    }

    @Override
    public int getWidth() {
        return Gdx.graphics.getWidth();
    }

    @Override
    public void setWidth(int width) {
        Gdx.graphics.setWindowedMode(width, this.getHeight());
    }

    @Override
    public int getHeight() {
        return Gdx.graphics.getHeight();
    }

    @Override
    public void setHeight(int height) {
        Gdx.graphics.setWindowedMode(this.getWidth(), height);
    }

    /**
     * Initialized window.
     */
    @Override
    public synchronized void init() {
        if (this.initialized) {
            throw new OneTimeUseException("The game window is already initialized.");
        }
        this.initialized = true;

        BubbleBlaster.getLogger().info(MARKER, "Initialized game window");

        this.game().windowLoaded();
    }

    @Override
    public void dispose() {
        this.game().shutdown();
    }

    @Override
    public Cursor registerCursor(int hotSpotX, int hotSpotY, Identifier identifier) {
        return Gdx.graphics.newCursor(new Pixmap(BubbleBlaster.resource(identifier.mapPath(s -> "textures/cursor/" + s + ".png"))), hotSpotX, hotSpotY);
    }

    @Override
    @Deprecated(forRemoval = true)
    public void finalSetup() {

    }

    @Override
    public boolean toggleFullscreen() {
        this.setFullscreen(!this.isFullscreen());
        return false;
    }

    @Override
    public boolean setFullscreen(boolean enable) {
        if (this.isFullscreen() && !enable) {
            if (!WindowEvents.WINDOW_FULLSCREEN.factory().onWindowFullscreen(this, false).isCanceled()) {
                this.setVisible(true);
                Gdx.graphics.setWindowedMode(Constants.DEFAULT_SIZE.x, Constants.DEFAULT_SIZE.y);
                this.requestFocus();
            }
        } else if (!this.isFullscreen() && enable) {
            if (!WindowEvents.WINDOW_FULLSCREEN.factory().onWindowFullscreen(this, true).isCanceled()) {
                this.setVisible(true);
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                var mode = Gdx.graphics.getDisplayMode();
                this.game().resize(mode.width, mode.height);
                this.requestFocus();
            }
        }

        BubbleBlasterConfig.FULLSCREEN.set(enable);
        BubbleBlasterConfig.save();
        return enable;
    }

    @Override
    public void setVisible(boolean visible) {

    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(0, 0, this.getWidth(), this.getHeight());
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public void requestFocus() {

    }

    @Override
    public boolean isFocused() {
        return true;
    }

    /**
     * Taskbar feature, flashes the taskbar icon on Windows.
     * Other operating systems are unknown for this behavior.
     */
    @Override
    public void requestUserAttention() {

    }

    @SuppressWarnings({"FieldCanBeLocal"})
    public static class Properties {
        private final int width;
        private final int height;
        private final String title;
        private boolean fullscreen;
        private Runnable onClose = () -> {};

        public Properties(@NotNull String title, int width, int height) {
            if (width < 0) throw new IllegalArgumentException("Width is negative");
            if (height < 0) throw new IllegalArgumentException("Height is negative");
            Objects.requireNonNull(title, "Title is set to null");

            this.width = width;
            this.height = height;
            this.title = title;
        }

        @Contract("->this")
        public Properties fullscreen() {
            this.fullscreen = true;
            return this;
        }

        public Properties close(Runnable onClose) {
            this.onClose = onClose;
            return this;
        }
    }
}
