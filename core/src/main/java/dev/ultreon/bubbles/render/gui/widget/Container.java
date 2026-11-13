package dev.ultreon.bubbles.render.gui.widget;

import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.GuiComponent;
import dev.ultreon.bubbles.render.gui.GuiStateListener;
import dev.ultreon.bubbles.render.gui.RenderableListener;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class Container extends GuiComponent {
    protected final List<GuiComponent> children = new CopyOnWriteArrayList<>();
    protected final List<RenderableListener> statics = new CopyOnWriteArrayList<>();
    int innerXOffset;
    int innerYOffset;
    protected GuiComponent hovered;
    protected GuiComponent pressed;
    protected GuiComponent focused;

    public Container(int x, int y,  int width,  int height) {
        super(x, y, width, height);
    }

    @Override
    public void render(Renderer renderer, int mouseX, int mouseY, float deltaTime) {
        var focused = this.focused;
        if (focused != null && !focused.enabled) {
            focused.onFocusLost();
            this.focused = null;
        }
        renderer.scissored(this.getBounds(), () -> this.renderChildren(renderer, mouseX, mouseY, deltaTime));
    }

    protected void renderChildren(Renderer renderer, int mouseX, int mouseY, float deltaTime) {
        for (var child : this.children)
            if (child.visible) {
                renderer.scissored(child.getBounds(), () -> child.render(renderer, mouseX, mouseY, deltaTime));
            }
    }

    @Nullable
    public GuiComponent getExactWidgetAt(int x, int y) {
        var widgetAt = this.getWidgetAt(x, y);
        if (widgetAt instanceof Container) {
            var container = (Container) widgetAt;
            return container.getExactWidgetAt(x, y);
        }
        return widgetAt;
    }

    /**
     * Adds a {@link GuiComponent gui component } to the screen, including initializing it with {@link GuiStateListener#make()}.
     *
     * @param child the gui component to add.
     * @param <T>   the component's type.
     * @return the same as the parameter.
     */
    public <T extends GuiComponent> T add(T child) {
        this.children.add(child);
        child.make();
        return child;
    }

    public void remove(GuiComponent child) {
        this.children.remove(child);
    }

    @Nullable
    public GuiComponent getWidgetAt(int x, int y) {
        var guiComponents = this.children;
        for (var i = guiComponents.size() - 1; i >= 0; i--) {
            var child = guiComponents.get(i);
            if (!child.enabled || !child.visible) continue;
            if (child.isWithinBounds(x, y)) return child;
        }
        return null;
    }

    @Override
    public boolean mouseClick(int x, int y, int button, int count) {
        var widgetAt = this.getWidgetAt(x, y);
        return widgetAt != null && widgetAt.mouseClick(x - widgetAt.getX(), y - widgetAt.getY(), button, count);
    }

    @Override
    public boolean mousePress(int x, int y, int button) {
        var widgetAt = this.getWidgetAt(x, y);

        // Pressing widget.
        this.pressed = widgetAt;

        // Focus
        var widgetChanged = false;

        if (this.focused != null && !this.focused.isWithinBounds(x, y))
            this.focused.onFocusLost();

        if (widgetAt != this.focused)
            widgetChanged = true;

        if (widgetAt == null || widgetAt.enabled)
            this.focused = widgetAt;

        if (this.focused != null && widgetChanged)
            this.focused.onFocusGained();

        // Widget handling.
        return widgetAt != null && widgetAt.mousePress(x, y, button);
    }

    @Override
    public boolean mouseRelease(int x, int y, int button) {
        var widgetAt = this.pressed;
        x -= this.x + this.innerXOffset;
        y -= this.y + this.innerYOffset;
        return widgetAt != null && widgetAt.mouseRelease(x, y, button);
    }

    @Override
    public void mouseEnter(int x, int y) {
        var widgetAt = this.getWidgetAt(x, y);
        var widgetChanged = false;
        if (this.hovered != null && !this.hovered.isWithinBounds(x, y)) {
            this.hovered.mouseExit();
        }

        if (widgetAt != this.hovered) widgetChanged = true;
        this.hovered = widgetAt;

        if (this.hovered != null) {
            x -= this.x + this.innerXOffset;
            y -= this.y + this.innerYOffset;
            if (widgetChanged) {
                this.hovered.mouseEnter(x - widgetAt.getX(), y - widgetAt.getY());
            }
        }
        super.mouseMove(x, y);
    }

    @Override
    public void mouseDrag(int x, int y, int nx, int ny, int button) {
        var widgetAt = this.getWidgetAt(x, y);
        x -= this.x + this.innerXOffset;
        y -= this.y + this.innerYOffset;
        nx -= this.x + this.innerXOffset;
        ny -= this.y + this.innerYOffset;
        if (widgetAt != null) widgetAt.mouseDrag(x - widgetAt.getX(), y - widgetAt.getY(), nx, ny, button);
    }

    @Override
    public void mouseExit() {
        if (this.hovered != null) {
            this.hovered.mouseExit();
            this.hovered = null;
        }
    }

    @Override
    public boolean mouseWheel(int x, int y, float rotation) {
        var widgetAt = this.getWidgetAt(x, y);
        x -= this.x + this.innerXOffset;
        y -= this.y + this.innerYOffset;
        if (widgetAt != null) return widgetAt.mouseWheel(x - widgetAt.getX(), y - widgetAt.getY(), rotation);
        return false;
    }

    protected final void clearWidgets() {
        for (var widget : this.children)
            widget.dispose();
        this.children.clear();
    }

    public GuiComponent getHoveredWidget() {
        return this.hovered;
    }
}
