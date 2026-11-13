package dev.ultreon.bubbles.render.gui.widget;

import dev.ultreon.bubbles.render.Renderer;

public class Panel extends Container {
    /**
     * @param x      position create the widget
     * @param y      position create the widget
     * @param width  size create the widget
     * @param height size create the widget
     */
    public Panel(int x, int y,  int width,  int height) {
        super(x, y, width, height);
    }

    @Override
    public void render(Renderer renderer, int mouseX, int mouseY, float deltaTime) {
        this.renderComponent(renderer);
        this.renderChildren(renderer, mouseX, mouseY, deltaTime);
    }

    @Override
    public void renderComponent(Renderer renderer) {
        renderer.fill(0, 0, this.width, this.height, this.getBackgroundColor());
    }

}
