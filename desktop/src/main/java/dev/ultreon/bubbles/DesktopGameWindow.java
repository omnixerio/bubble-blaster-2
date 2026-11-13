package dev.ultreon.bubbles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Rectangle;
import dev.ultreon.bubbles.event.v1.WindowEvents;
import dev.ultreon.bubbles.util.exceptions.OneTimeUseException;
import dev.ultreon.libs.commons.v0.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Window class for the game's window.
 *
 * @author XyperCode
 */
@SuppressWarnings("unused")
public class DesktopGameWindow implements GameWindow {
    private static final Marker MARKER = MarkerFactory.getMarker("GameWindow");

    private boolean initialized = false;

    
    private int fps;
    private Thread mainThread;
    private boolean visible;

    /**
     * Window constructor.
     */
    public DesktopGameWindow(Properties properties) {
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
        this.getLwjglWindow().setWindowListener(new GameWindowAdapter());

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
        var window = this.getLwjglWindow();
        window.setVisible(visible);
        this.visible = visible;

        if (visible) window.focusWindow();
    }

    private Lwjgl3Window getLwjglWindow() {
        return ((Lwjgl3Graphics) Gdx.graphics).getWindow();
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
        return this.getLwjglWindow().getPositionX();
    }

    @Override
    public int getY() {
        return this.getLwjglWindow().getPositionY();
    }

    @Override
    public void requestFocus() {
        this.getLwjglWindow().focusWindow();
    }

    @Override
    public boolean isFocused() {
        return this.getLwjglWindow().isFocused();
    }

    /**
     * Taskbar feature, flashes the taskbar icon on Windows.
     * Other operating systems are unknown for this behavior.
     */
    @Override
    public void requestUserAttention() {
        this.getLwjglWindow().flash();
    }

    public void showError(@NotNull String title, @Nullable String description) {

    }

    private class GameWindowAdapter implements Lwjgl3WindowListener {
        @Override
        public void created(Lwjgl3Window window) {
            WindowEvents.WINDOW_CREATED.factory().onWindowCreated(DesktopGameWindow.this);
        }

        @Override
        public void iconified(boolean isIconified) {
            if (isIconified) WindowEvents.WINDOW_MINIMIZED.factory().onWindowMinimize(DesktopGameWindow.this);
            else WindowEvents.WINDOW_MINIMIZED_RESTORE.factory().onWindowMinimize(DesktopGameWindow.this);
        }

        @Override
        public void maximized(boolean isMaximized) {
            if (isMaximized) WindowEvents.WINDOW_MAXIMIZED.factory().onWindowMaximize(DesktopGameWindow.this);
            else WindowEvents.WINDOW_MAXIMIZED_RESTORE.factory().onWindowMaximize(DesktopGameWindow.this);
        }

        @Override
        public void focusLost() {
            WindowEvents.WINDOW_LOST_FOCUS.factory().onWindowLostFocus(DesktopGameWindow.this);
        }

        @Override
        public void focusGained() {
            WindowEvents.WINDOW_GAINED_FOCUS.factory().onWindowGainedFocus(DesktopGameWindow.this);
        }

        @Override
        public boolean closeRequested() {
            if (WindowEvents.WINDOW_CLOSING.factory().onWindowClosing(DesktopGameWindow.this).isCanceled()) {
                return false;
            }
            DesktopGameWindow.this.game().shutdown();
            return false;
        }

        @Override
        public void filesDropped(String[] files) {
            WindowEvents.WINDOW_FILES_DROPPED.factory().onWindowFilesDropped(DesktopGameWindow.this, files);
        }

        @Override
        public void refreshRequested() {

        }
    }
}
