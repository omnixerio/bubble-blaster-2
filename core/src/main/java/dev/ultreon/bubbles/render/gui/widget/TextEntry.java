package dev.ultreon.bubbles.render.gui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.ultreon.bubbles.config.Config;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Insets;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.GuiComponent;
import dev.ultreon.libs.commons.v0.Mth;
import dev.ultreon.libs.commons.v0.size.IntSize;
import dev.ultreon.libs.commons.v0.vector.Vec2i;
import dev.ultreon.bubbles.text.TextObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class TextEntry extends GuiComponent {
    // Cursor Index/
    protected final GlyphLayout layout = new GlyphLayout();
    protected int cursorIndex;

    @NotNull
    protected String text = "";
    protected TextObject label;

    protected boolean activated;
    protected int entryWidth;

    private Predicate<String> responder = _ -> true;
    String validText;

    protected TextEntry(int x, int y,  int width,  int height) {
        super(x, y, width, height);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean mousePress(int x, int y, int button) {
        if (button == Buttons.LEFT) {
            this.activated = this.isWithinBounds(x, y);
            return true;
        }

        return super.mousePress(x, y, button);
    }

    @Override
    public boolean keyPress(int keyCode) {
        if (!this.activated) return false;

        if (keyCode == Input.Keys.BACKSPACE) {
            if (this.text.isEmpty()) return false;

            var leftText = this.text.substring(0, this.cursorIndex - 1);
            var rightText = this.text.substring(this.cursorIndex);

            var text = leftText + rightText;
            if (this.responder.test(text)) this.validText = text;
            this.text = text;
            this.cursorIndex = Mth.clamp(this.cursorIndex - 1, 0, this.text.length());
            this.updateCursor();
            return true;
        }

        if (keyCode == Input.Keys.FORWARD_DEL) {
            if (this.text.isEmpty()) return false;
            if (this.cursorIndex >= this.text.length() - 1) return false;

            var leftText = this.text.substring(0, this.cursorIndex);
            var rightText = this.text.substring(this.cursorIndex + 1);

            var text = leftText + rightText;
            if (this.responder.test(text)) this.validText = text;
            this.text = text;
            this.cursorIndex = Mth.clamp(this.cursorIndex - 1, 0, this.text.length());
            this.updateCursor();
            return true;
        }

        if (keyCode == Input.Keys.LEFT) {
            this.cursorIndex = Mth.clamp(this.cursorIndex - 1, 0, this.text.length());
            this.updateCursor();
            return true;
        }

        if (keyCode == Input.Keys.RIGHT) {
            this.cursorIndex = Mth.clamp(this.cursorIndex + 1, 0, this.text.length());
            this.updateCursor();
            return true;
        }

        if (keyCode == Input.Keys.ENTER) {
            this.text = this.validText;
            return true;
        }
        return false;
    }

    @Override
    public boolean charType(char character) {
        if ((short) character >= 32) {
            var leftText = this.text.substring(0, this.cursorIndex);
            var rightText = this.text.substring(this.cursorIndex);

            var text = leftText + character + rightText;
            if (this.responder.test(text)) this.validText = text;
            this.text = text;
            this.layout.setText(this.font, text.substring(0, this.cursorIndex));
            this.cursorIndex++;
            this.updateCursor();

            return true;
        }

        return false;
    }

    @Override
    public void onFocusLost() {
        super.onFocusLost();

        Gdx.input.setOnscreenKeyboardVisible(false);

        this.text = this.validText;
    }

    @Override
    public void onFocusGained() {
        super.onFocusGained();

        Gdx.input.setOnscreenKeyboardVisible(true, this.getOnscreenKeyboardType());
    }

    public Input.OnscreenKeyboardType getOnscreenKeyboardType() {
        return Input.OnscreenKeyboardType.Default;
    }

    protected void updateCursor() {
        this.layout.setText(this.font, this.text.substring(0, Mth.clamp(this.cursorIndex, 0, this.text.length())));
    }

    @Override
    public void render(Renderer renderer, int mouseX, int mouseY, float deltaTime) {
        final var entryX = Math.max(this.x + this.width - this.entryWidth, this.x);
        final var entryW = Math.min(this.entryWidth, this.width);
        final var labelX = this.x;
        final var labelW = Math.max(this.width - entryW, 0);

        this.drawBackground(renderer, labelX, labelW, entryX, entryW);
        this.drawText(renderer, entryX, entryW, labelX, labelW);
    }

    protected void drawBackground(Renderer renderer, int labelX, int labelW, int entryX, int entryW) {
        if (this.isFocused()) {
            renderer.fill(labelX, this.y, labelW, this.height, Colors.rgba(0xff, 0xff, 0xff, 0x40));
            renderer.fill(entryX, this.y, entryW, this.height, Colors.rgba(0xff, 0xff, 0xff, 0x80));

            if (this.isError()) renderer.drawErrorEffectBox(this.x, this.y, this.width, this.height, new Insets(1, 1, 4, 1));
            else renderer.drawEffectBox(this.x, this.y, this.width, this.height, new Insets(0, 0, 4, 0));
        } else if (!this.enabled) {
            renderer.fill(labelX, this.y, labelW, this.height, Colors.rgba(0xff, 0xff, 0xff, 0x10));
            renderer.fill(entryX, this.y, entryW, this.height, Colors.rgba(0xff, 0xff, 0xff, 0x30));
            if (this.isError()) renderer.drawErrorEffectBox(this.x, this.y, this.width, this.height, new Insets(1));
        } else {
            renderer.fill(labelX, this.y, labelW, this.height, Colors.rgba(0xff, 0xff, 0xff, 0x20));
            renderer.fill(entryX, this.y, entryW, this.height, Colors.rgba(0xff, 0xff, 0xff, 0x60));
            if (this.isError()) renderer.drawErrorEffectBox(this.x, this.y, this.width, this.height, new Insets(1));
        }
    }

    protected void drawText(Renderer renderer, int entryX, int entryW, int labelX, int labelW) {
        renderer.scissored(entryX + 2, this.y, entryW - 4, this.height, () -> {
            if (this.enabled)
                renderer.drawTextLeft(this.font, this.text, entryX + 10, this.y + this.getHeight() / 2f - 2f, Colors.WHITE);
            else
                renderer.drawTextLeft(this.font, this.text, entryX + 10, this.y + this.getHeight() / 2f - 2f, Colors.rgba(0xff, 0xff, 0xff, 0x60));

            if (this.isFocused()) {
                var cursorX = this.text.isEmpty() ? entryX + 10 : entryX + 10 + this.layout.width;
                renderer.fillEffect(cursorX, this.y + this.getHeight() / 2f - this.font.getLineHeight() / 2, 2, this.font.getLineHeight());
            }
        });

        renderer.scissored(labelX + 2, this.y, labelW - 4, this.isFocused() ? this.height - 4 : this.height, () -> renderer.drawTextCenter(this.font, this.label, labelX + labelW / 2f , this.y + this.getHeight() / 2f - 2f, Colors.WHITE));
    }

    public void setText(@NotNull String text) {
        if (this.responder.test(text)) this.validText = text;
        this.text = text;
        this.cursorIndex = this.text.length();
        this.updateCursor();
    }

    @NotNull
    public String getText() {
        return this.text;
    }

    @Nullable
    public String getValidText() {
        return this.validText;
    }

    public void setResponder(@NotNull Consumer<@NotNull String> responder) {
        this.responder = (@NotNull String text) -> {
            responder.accept(text);
            return true;
        };
    }

    public void setResponder(@NotNull Predicate<@NotNull String> responder) {
        this.responder = responder;
    }

    public void setEntryWidth(int entryWidth) {
        this.entryWidth = entryWidth;
    }

    public int getEntryWidth() {
        return this.entryWidth;
    }

    public void setLabel(TextObject label) {
        this.label = label;
    }

    public TextObject getLabel() {
        return this.label;
    }

    public boolean isError() {
        return !this.text.equals(this.validText);
    }

    protected void validate() {
        var text = this.text;
        if (this.responder.test(text)) this.validText = text;
    }

    public static class Builder {
        public Rectangle bounds = null;
        @Nullable
        private String text = "";
        private int entryWidth = -1;
        private TextObject label = TextObject.EMPTY;
        private Predicate<String> responder = _ -> true;

        public Builder() {

        }

        public  TextEntry build() {
            if (this.bounds == null) throw new IllegalArgumentException("Missing bounds for creating text entry.");
            if (this.entryWidth == -1) throw new IllegalArgumentException("Missing entry width for creating text entry.");

            var entry = new TextEntry((int) this.bounds.x, (int) this.bounds.y, (int) this.bounds.width, (int) this.bounds.height);
            entry.setText(this.text == null ? "" : this.text);
            entry.setEntryWidth(this.entryWidth);
            entry.setLabel(this.label);
            entry.setResponder(this.responder);

            return entry;
        }

        public  Builder bounds(Rectangle bounds) {
            this.bounds = bounds;
            return this;
        }

        public  Builder bounds(int x, int y, int width, int height) {
            this.bounds = new Rectangle(x, y, width, height);
            return this;
        }

        public  Builder bounds(Vec2i pos, IntSize size) {
            this.bounds = new Rectangle(pos.x, pos.y, size.width(), size.height());
            return this;
        }

        public  Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder text(Config.StringEntry entry) {
            this.text = entry.get();
            return this;
        }

        @CanIgnoreReturnValue
        public  Builder entryWidth(int width) {
            if (width < 0) throw new IllegalArgumentException("Entry width should be non-negative.");
            this.entryWidth = width;
            return this;
        }

        @CanIgnoreReturnValue
        public  Builder responder(Predicate<@NotNull String> responder) {
            this.responder = responder;
            return this;
        }

        @CanIgnoreReturnValue
        public  Builder responder(Consumer<@NotNull String> responder) {
            this.responder = s -> {
                responder.accept(s);
                return true;
            };
            return this;
        }

        public  Builder label(@Nullable String label) {
            return this.label(TextObject.nullToEmpty(label));
        }

        public  Builder label(TextObject label) {
            this.label = label;
            return this;
        }
    }
}
