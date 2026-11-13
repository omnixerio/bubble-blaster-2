package dev.ultreon.bubbles.render.gui.widget;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import dev.ultreon.bubbles.init.Fonts;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Insets;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.GuiStateListener;
import dev.ultreon.bubbles.text.TextObject;

public class Button extends AbstractButton implements GuiStateListener {
    protected TextObject text;

    public static Builder builder() {
        return new Builder();
    }

    public void setText(String text) {
        this.text = TextObject.literal(text);
    }

    public void setText(TextObject text) {
        this.text = text;
    }

    public TextObject getText() {
        return this.text;
    }

    public static class Builder {
        protected Rectangle bounds = new Rectangle(10, 10, 96, 48);
        protected TextObject text = TextObject.EMPTY;
        protected Runnable command = () -> {
        };
        protected BitmapFont font = Fonts.DEFAULT.get();

        protected Builder() {
        }

        public Button build() {
            var button = new Button((int) this.bounds.x, (int) this.bounds.y, (int) this.bounds.width, (int) this.bounds.height);

            button.setText(this.text);
            button.setCommand(this.command);
            button.setFont(this.font);
            return button;
        }

        public Builder bounds(Rectangle bounds) {
            this.bounds = bounds;
            return this;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.bounds = new Rectangle(x, y, width, height);
            return this;
        }

        public Builder text(String text) {
            this.text = TextObject.literal(text);
            return this;
        }

        public Builder text(TextObject text) {
            this.text = text;
            return this;
        }

        public Builder command(Runnable command) {
            this.command = command;
            return this;
        }

        public  Builder font(BitmapFont font) {
            this.font = font;
            return this;
        }
    }

    protected Button(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public void render(Renderer renderer, int mouseX, int mouseY, float deltaTime) {
        if (this.isPressed()) this.backgroundColor = Colors.rgba(0xff, 0xff, 0xff, 0x40);
        else if (this.isHovered()) this.backgroundColor = Colors.rgba(0xff, 0xff, 0xff, 0x30);
        else this.backgroundColor = Colors.rgba(0xff, 0xff, 0xff, 0x20);

        if (this.enabled)
            renderer.fill(this.getBounds(), this.backgroundColor);
        else
            renderer.fill(this.getBounds(), Colors.rgba(0xff, 0xff, 0xff, 0x10));

        if (this.isHovered() && this.enabled) {
            renderer.hovered();
            renderer.drawEffectBox(this.x, this.y, this.width, this.height, new Insets(0, 0, 4, 0));
        }

        this.drawText(renderer);
    }

    protected void drawText(Renderer renderer) {
        AbstractButton.drawText(renderer, this.enabled ? Colors.WHITE : Colors.rgba(0xff, 0xff, 0xff, 0x60), this.getPos(), this.getSize(), this.getText(), this.getFont());
    }
}
