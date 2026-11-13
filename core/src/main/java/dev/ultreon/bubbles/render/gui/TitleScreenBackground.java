package dev.ultreon.bubbles.render.gui;

import com.badlogic.gdx.math.Vector2;
import dev.ultreon.bubbles.bubble.BubbleType;
import dev.ultreon.bubbles.init.BubbleTypes;
import dev.ultreon.bubbles.random.JavaRandom;
import dev.ultreon.bubbles.random.RandomSource;
import dev.ultreon.bubbles.random.valuesource.ValueSource;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.world.WorldRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TitleScreenBackground {
    private static final BubbleType TYPE = BubbleTypes.NORMAL;
    private static final int MAX_BUBBLES = 500;
    private final RandomSource random = new JavaRandom();
    private final List<FakeBubble> bubbles = new CopyOnWriteArrayList<>();
    private final int width;
    private final int height;

    public TitleScreenBackground(int width, int height) {
        this.width = width;
        this.height = height;

        for (var i = 0; i <= MAX_BUBBLES; i++) {
            this.addBubbleInit();
        }
    }

    private void addBubble() {
        var fakeBubble = new FakeBubble(TYPE.getRadius(), TYPE.getSpeed());
        fakeBubble.position.x = this.width + fakeBubble.radius;
        fakeBubble.position.y = this.random.nextFloat(-fakeBubble.radius, this.height + fakeBubble.radius);
        this.bubbles.add(fakeBubble);
    }

    private void addBubbleInit() {
        var fakeBubble = new FakeBubble(TYPE.getRadius(), TYPE.getSpeed());
        fakeBubble.position.x = this.random.nextFloat(-fakeBubble.radius, this.width + fakeBubble.radius);
        fakeBubble.position.y = this.random.nextFloat(-fakeBubble.radius, this.height + fakeBubble.radius);
        this.bubbles.add(fakeBubble);
    }

    public void tick() {
        List<FakeBubble> toRemove = new ArrayList<>();
        for (var bubble : this.bubbles) {
            bubble.position.add(-bubble.speed, 0);
            if (bubble.position.x < -bubble.radius) {
                toRemove.add(bubble);
            }
        }

        this.bubbles.removeAll(toRemove);

        if (this.bubbles.size() < MAX_BUBBLES) {
            this.addBubble();
        }
    }

    public void render(Renderer renderer) {
        renderer.blurred(true, () -> {
            renderer.fillGradient(0, 0, this.width, this.height, WorldRenderer.BG_TOP, WorldRenderer.BG_BOTTOM);

            for (var fakeBubble : this.bubbles)
                WorldRenderer.drawBubble(renderer, fakeBubble.position.x, fakeBubble.position.y, fakeBubble.radius, 0, TYPE, false);
        });

        renderer.fillGradient(0, 0, this.width, this.height, Colors.rgba(0x00, 0x00, 0x00, 0x80), Colors.rgba(0x00, 0x00, 0x00, 0x90));
    }

    public void dispose() {
        this.bubbles.clear();
    }

    private static class FakeBubble {
        private final float radius;
        private final float speed;
        private final Vector2 position = new Vector2();

        public FakeBubble(ValueSource radius, ValueSource speed) {
            this.radius = (float) radius.getValue();
            this.speed = (float) speed.getValue();
        }
    }
}
