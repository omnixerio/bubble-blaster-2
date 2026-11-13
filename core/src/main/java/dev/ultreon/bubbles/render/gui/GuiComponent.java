package dev.ultreon.bubbles.render.gui;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.init.Fonts;
import dev.ultreon.bubbles.init.SoundEvents;
import dev.ultreon.bubbles.input.DesktopInput;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.util.Functions;
import dev.ultreon.libs.commons.v0.vector.Vec2i;

import java.util.Objects;

/**
 * Controllable widget, a widget that can be controlled by the user.
 * This widget contains input event handlers like {@link #keyPress(int)} and {@link #mouseClick(int, int, int, int)}
 *
 * @author XyperCode
 */
public abstract class GuiComponent implements GuiStateListener, RenderableListener {
    protected final BubbleBlaster game = BubbleBlaster.getInstance();
    protected final GlyphLayout layout = new GlyphLayout();

    protected BitmapFont font = Fonts.DEFAULT.get();
    public final BitmapFont monospaced = Functions.tryGet(Fonts.MONOSPACED).getLeftOrNull();

    protected volatile int x;
    protected volatile int y;
    protected volatile int width;
    protected volatile int height;

    protected Color backgroundColor = new Color(1f, 1f, 1f, 0.1254902f);

    public boolean enabled = true;
    public boolean visible = true;

    private boolean valid;
    private final long hash;

    private boolean focused = false;

    private int lastMouseX;
    private int lastMouseY;

    /**
     * @param x      position of the widget
     * @param y      position of the widget
     * @param width  size of the widget
     * @param height size of the widget
     */
    @SuppressWarnings("ConstantValue")
    public GuiComponent(int x, int y,  int width,  int height) {
        if (width < 0) throw new IllegalArgumentException("Width is negative");
        if (height < 0) throw new IllegalArgumentException("Height is negative");

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.hash = System.nanoTime();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        var that = (GuiComponent) o;
        return this.hash == that.hash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hash);
    }

    /**
     * Rendering method, should not be called unless you know what you are doing.
     * Override is fine.
     *
     * @param renderer the renderer to render with.
     * @param mouseX the X position of the mouse pointer.
     * @param mouseY the Y position of the mouse pointer.
     * @param deltaTime the game's {@linkplain Graphics#getDeltaTime() delta time}.
     */
    @Override
    public abstract void render(Renderer renderer, int mouseX, int mouseY, float deltaTime);

    /**
     * Handler for mouse clicking.<br>
     * Should only be overridden, and not called unless you know what you are doing.
     *
     * @param x      the x position when clicked.
     * @param y      the y position when clicked.
     * @param button the button used.
     * @param count  the amount of sequential clicks.
     */
    public boolean mouseClick(int x, int y, int button, int count) {
        return false;
    }

    /**
     * Handler for mouse button press.<br>
     * Should only be overridden, and not called unless you know what you are doing.
     *
     * @param x      the x position when pressed.
     * @param y      the y position when pressed.
     * @param button the button used.
     */
    public boolean mousePress(int x, int y, int button) {
        return false;
    }

    /**
     * Handler for mouse button release.<br>
     * Should only be overridden, and not called unless you know what you are doing.
     *
     * @param x      the x position when released.
     * @param y      the y position when released.
     * @param button the button used.
     */
    public boolean mouseRelease(int x, int y, int button) {
        return false;
    }

    /**
     * Handler for mouse motion.<br>
     * Should only be overridden, and not called unless you know what you are doing.
     *
     * @param x the x position where the mouse moved to.
     * @param y the y position where the mouse moved to.
     */
    public void mouseMove(int x, int y) {
        this.lastMouseX = x;
        this.lastMouseY = y;
    }

    /**
     * Handler for mouse pressing.<br>
     * Should only be overridden, and not called unless you know what you are doing.
     *
     * @param x      the x position when pressed.
     * @param y      the y position when pressed.
     * @param nx     the x position dragged to.
     * @param ny     the y position dragged to.
     * @param button the button used.
     */
    public void mouseDrag(int x, int y, int nx, int ny, int button) {

    }

    /**
     * Called when the mouse exits the widget.
     */
    public void mouseExit() {

    }

    /**
     * Called when the mouse enters the widget.
     *
     * @param x x position where it entered.
     * @param y y position where it entered.
     */
    public void mouseEnter(int x, int y) {

    }

    public boolean mouseWheel(int x, int y, float rotation) {
        return false;
    }

    /**
     * Key press handler.
     * Match a constant {@link Input.Keys} with the {@code keyCode} parameter for checking which key is pressed.
     *
     * @param keyCode   the code for the key pressed.
     * @return to cancel out other usage of this method.
     */
    public boolean keyPress(int keyCode) {
        return false;
    }

    /**
     * Key release handler.
     * Match a constant {@link Input.Keys} with the {@code keyCode} parameter for checking which key is released.
     *
     * @param keyCode   the code for the key released.
     * @return to cancel out other usage of this method.
     */
    public boolean keyRelease(int keyCode) {
        return false;
    }

    /**
     * Key type handler.
     * Match a constant {@link Input.Keys} with the {@code keyCode} parameter for checking which key is typed.
     *
     * @param character the character typed.
     * @return to cancel out other usage of this method.
     */
    public boolean charType(char character) {
        return false;
    }

    @Override
    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        if (width < 0) {
            throw new IllegalArgumentException("Width should be positive.");
        }
        this.width = width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        if (this.width < 0) {
            throw new IllegalArgumentException("Height should be positive.");
        }
        this.height = height;
    }

    @Override
    public Vec2i getPos() {
        return new Vec2i(this.x, this.y);
    }

    public void setPos(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    public void setPos(Vec2i pos) {
        this.setPos(pos.x, pos.y);
    }

    @Override
    public Vec2i getSize() {
        return new Vec2i(this.width, this.height);
    }

    public void setSize(Vec2i size) {
        this.setSize(size.x, size.y);
    }

    public void setSize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.setPos(x, y);
        this.setSize(width, height);
    }

    public void setBounds(Rectangle bounds) {
        this.x = (int) bounds.x;
        this.y = (int) bounds.y;
        this.width = (int) bounds.width;
        this.height = (int) bounds.height;
    }

    public Color getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = Colors.argb(color);
    }

    public void setBackgroundColor(int red, int green, int blue) {
        this.backgroundColor = Colors.rgb(red, green, blue);
    }

    public void setBackgroundColor(int red, int green, int blue, int alpha) {
        this.backgroundColor = Colors.rgba(red, green, blue, alpha);
    }

    public void setBackgroundColor(float red, float green, float blue) {
        this.backgroundColor = Colors.rgb(red, green, blue);
    }

    public void setBackgroundColor(float red, float green, float blue, float alpha) {
        this.backgroundColor = Colors.rgba(red, green, blue, alpha);
    }

    public void setBackgroundColor(String hex) {
        this.backgroundColor = Colors.hex(hex);
    }

    /**
     * Check if a position is withing the bounds of the widget
     *
     * @param x position to check for.
     * @param y position to check for.
     * @return true if the x and y position given is withing the bounds of the widget
     */
    public boolean isWithinBounds(int x, int y) {
        return this.getBounds().contains(x, y);
    }

    /**
     * Check if a position is withing the bounds of the widget
     *
     * @param pos position to check for.
     * @return true if the x and y position given is withing the bounds of the widget
     */
    public boolean isWithinBounds(Vector2 pos) {
        return this.getBounds().contains(pos);
    }

    public void renderComponent(Renderer renderer) {

    }

    public void onFocusGained() {
        this.focused = true;
    }

    public void onFocusLost() {
        this.focused = false;
    }

    public boolean isFocused() {
        return this.focused;
    }

    public void tick() {

    }

    @Override
    public void make() {
        this.valid = true;
    }

    @Override
    public void dispose() {
        this.valid = false;
    }

    @Override
    @Deprecated(forRemoval = true)
    public boolean isValid() {
        return true;
    }

    public boolean isHovered() {
        return this.isWithinBounds(DesktopInput.getMousePos());
    }

    protected final int getLastMouseX() {
        return this.lastMouseX;
    }

    protected final int getLastMouseY() {
        return this.lastMouseY;
    }


    @Deprecated(forRemoval = true)
    public static void fill(Renderer renderer, int x, int y, int width, int height, int color) {
        renderer.fill(x, y, width, height, Colors.argb(color));
    }

    @Deprecated(forRemoval = true)
    public static void fill(Renderer renderer, int x, int y, int width, int height, Color color) {
        renderer.fill(x, y, width, height, color);
    }

    protected void playMenuEvent() {
        SoundEvents.MENU_EVENT.play(0.2f);
    }

    public BitmapFont getFont() {
        return this.font;
    }

    public void setFont(BitmapFont font) {
        this.font = font;
    }
}
