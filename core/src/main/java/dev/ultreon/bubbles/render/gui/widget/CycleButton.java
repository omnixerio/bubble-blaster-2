package dev.ultreon.bubbles.render.gui.widget;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import dev.ultreon.bubbles.input.DesktopInput;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.GuiStateListener;
import dev.ultreon.bubbles.util.Enums;
import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.bubbles.text.Translatable;

public class CycleButton<T extends Enum<T>> extends Button implements GuiStateListener {
    private final Class<T> componentType;
    private int index;
    private T value;

    protected CycleButton(Class<T> componentType, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.componentType = componentType;
        this.value = componentType.getEnumConstants()[this.index];
    }

    public static <T extends Enum<T>> Builder<T> builder(Class<T> componentType) {
        return new Builder<>(componentType);
    }

    public T getValue() {
        return this.value;
    }

    public TextObject getFullText() {
        return TextObject.literal("").append(this.text).append(this.text.getText().isBlank() ? "" : ": ")
                .append(this.value instanceof Translatable ? ((Translatable) this.value).getTranslation() : TextObject.literal(this.value.name()));
    }

    public void setValue(T value) {
        if (value == this.value)
            return;

        this.value = value;
        this.index = value.ordinal();
        super.click();
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        var minValue = -Enums.size(this.componentType);
        if (index < minValue) throw new IllegalArgumentException("Index below minimum value: " + index + " (min: " + minValue + ")");
        index += Enums.size(this.componentType);
        this.index = index % Enums.size(this.componentType);
        this.value = this.componentType.getEnumConstants()[this.index];
    }

    @Override
    protected void click() {
        if (DesktopInput.isShiftDown()) {
            this.setIndex(this.getIndex() - 1);
        }
        this.setIndex(this.getIndex() + 1);
        super.click();
    }

    @Override
    protected void drawText(Renderer renderer) {
        AbstractButton.drawText(renderer, Colors.WHITE, this.getPos(), this.getSize(), this.getFullText(), this.getFont());
    }

    public static class Builder<T extends Enum<T>> extends Button.Builder {
        private final Class<T> componentType;

        protected Builder(Class<T> componentType) {
            this.componentType = componentType;
            if (this.componentType.getEnumConstants().length == 0) {
                throw new IllegalArgumentException("Empty enums are not allowed.");
            }
        }

        @Override
        public Builder<T> bounds(Rectangle bounds) {
            this.bounds = bounds;
            return this;
        }

        @Override
        public Builder<T> bounds(int x, int y, int width, int height) {
            this.bounds = new Rectangle(x, y, width, height);
            return this;
        }

        @Override
        public Builder<T> text(String text) {
            this.text = TextObject.literal(text);
            return this;
        }

        @Override
        public Builder<T> text(TextObject text) {
            this.text = text;
            return this;
        }

        @Override
        public Builder<T> command(Runnable command) {
            this.command = command;
            return this;
        }

        @Override
        public  Builder<T> font(BitmapFont font) {
            this.font = font;
            return this;
        }

        @Override
        public CycleButton<T> build() {
            var button = new CycleButton<>(this.componentType, (int) this.bounds.x, (int) this.bounds.y, (int) this.bounds.width, (int) this.bounds.height);

            button.setText(this.text);
            button.setCommand(this.command);
            button.setFont(this.font);
            return button;
        }
    }
}
