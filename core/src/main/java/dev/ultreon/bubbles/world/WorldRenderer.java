package dev.ultreon.bubbles.world;

import box2dLight.ConeLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Circle;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.BubbleBlasterConfig;
import dev.ultreon.bubbles.bubble.BubbleType;
import dev.ultreon.bubbles.debug.Profiler;
import dev.ultreon.bubbles.entity.player.Player;
import dev.ultreon.bubbles.event.v1.RenderEvents;
import dev.ultreon.bubbles.init.StatusEffects;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderable;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.hud.HudType;
import dev.ultreon.libs.commons.v0.Mth;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.List;

public class WorldRenderer implements Renderable {
    public static final Color BG_TOP = Colors.argb(0xff008EDA);
    public static final Color BG_BOTTOM = Colors.argb(0xff004BA1);
    public static final Color BG_TOP_NIGHT = Colors.argb(0xff008EDA).mul(0.5f);
    public static final Color BG_BOTTOM_NIGHT = Colors.argb(0xff004BA1).mul(0.5f);
    private final BubbleBlaster game = BubbleBlaster.getInstance();
    private final Profiler profiler = this.game.profiler;

    private final RayHandler rayHandler;
    private final ConeLight coneLight;
    private static com.badlogic.gdx.physics.box2d.World lightWorld;

    public WorldRenderer() {
        WorldRenderer.lightWorld = new com.badlogic.gdx.physics.box2d.World(new com.badlogic.gdx.math.Vector2(0, 0), true);
        this.rayHandler = new RayHandler(WorldRenderer.lightWorld);
        this.rayHandler.setAmbientLight(0.1f, 0.1f, 0.1f, 1f);
        this.rayHandler.setBlurNum(3);
        this.rayHandler.setShadows(true);
        this.coneLight = new ConeLight(this.rayHandler, 64, new Color(1f, 1f, 1f, 1f), 400f, 0f, 0f, 0f, 45f);
        this.coneLight.setSoft(true);
    }
    
    /**
     * Get the light world for creating light-blocking bodies.
     * @return the light world, or null if not initialized.
     */
    public static com.badlogic.gdx.physics.box2d.World getLightWorld() {
        return lightWorld;
    }

    /**
     * Draw bubble.
     * Draws bubble on screen.
     *
     * @param renderer the graphics-2D instance
     * @param x        the x-coordinate.
     * @param y        the y-coordinate.
     * @param radius   the bubble radius (full width.).
     * @param colors   the bubble colors (on sequence).
     */
    @Deprecated(forRemoval = true)
    public static void drawBubble(Renderer renderer, float x, float y, float radius, List<Color> colors) {
        WorldRenderer.drawBubble(renderer, x, y, radius, colors, null);
    }

    /**
     * Draw bubble.
     * Draws bubble on screen.
     *
     * @param renderer the graphics-2D instance
     * @param x        the x-coordinate.
     * @param y        the y-coordinate.
     * @param radius   the bubble radius (full width.).
     * @param colors   the bubble colors (on sequence).
     */
    @Deprecated(forRemoval = true)
    public static void drawBubble(Renderer renderer, float x, float y, float radius, List<Color> colors, @Nullable Texture insideTexture) {
        WorldRenderer.drawBubble(renderer, x, y, radius, 0, colors, insideTexture);
    }

    /**
     * Draw bubble.
     * Draws bubble on screen.
     *
     * @param renderer     the graphics-2D instance
     * @param x            the x-coordinate.
     * @param y            the y-coordinate.
     * @param radius       the bubble radius (full width.).
     * @param destroyFrame
     * @param colors       the bubble colors (on sequence).
     */
    @Deprecated(forRemoval = true)
    public static void drawBubble(Renderer renderer, float x, float y, float radius, int destroyFrame, List<Color> colors) {
        WorldRenderer.drawBubble(renderer, x, y, radius, destroyFrame, colors, null);
    }

    /**
     * Draw bubble.
     * Draws bubble on screen.
     *
     * @param renderer      the graphics-2D instance
     * @param x      the x-coordinate.
     * @param y      the y-coordinate.
     * @param radius the bubble radius (full width.).
     * @param colors the bubble colors (on sequence).
     */
    @Deprecated(forRemoval = true)
    public static void drawBubble(Renderer renderer, float x, float y, float radius, int destroyFrame, List<Color> colors, Texture insideTexture) {
        float thickness = BubbleBlasterConfig.BUBBLE_LINE_THICKNESS.get();
        if (insideTexture == null && renderer.drawBubble(x, y, radius, thickness, thickness + 0.5f, colors, WorldRenderer.isNight())) {
            return;
        }

        // Define ellipse-depth (pixels).
        var i = 0f;
        var destroyFrame0 = Mth.clamp(destroyFrame, 0, 10);

        // Loop colors.
        for (var color : colors) {
            renderer.setLineThickness(thickness + 1 / 2f);

            // Draw singular circle in the circle list.
            var circle = WorldRenderer.getCircle(x, y, radius, i);
            renderer.circle(circle.x, circle.y, circle.radius, color);
            if (insideTexture != null) {
                renderer.setTexture(insideTexture);
                renderer.blit((int) (circle.x - circle.radius / 4), (int) (circle.y - circle.radius / 4), (int) circle.radius / 2, (int) circle.radius / 2);
            }

            i += thickness;
        }
    }

    /**
     * Draw bubble.
     * Draws bubble on screen.
     *
     * @param renderer      the graphics-2D instance
     * @param x      the x-coordinate.
     * @param y      the y-coordinate.
     * @param radius the bubble radius (full width.).
     * @param type   the type of bubble to render.
     */
    public static void drawBubble(Renderer renderer, float x, float y, float radius, int destroyFrame, BubbleType type) {
        float thickness = BubbleBlasterConfig.BUBBLE_LINE_THICKNESS.get();
        if (type.getInsideTexture() == null && renderer.drawBubble(x, y, radius, thickness, thickness + 0.5f, type.getColors(), isNight())) {
            return;
        }

        // Define ellipse-depth (pixels).
        var i = 0f;
        var destroyFrame0 = Mth.clamp(destroyFrame, 0, 10);

        // Loop colors.
        for (var color : type.getColors()) {
            renderer.setLineThickness(thickness + 1 / 2f);

            // Draw singular circle in the circle list.
            var circle = WorldRenderer.getCircle(x, y, radius, i);
            renderer.circle(circle.x, circle.y, circle.radius, color);
            var insideTexture = type.getInsideTexture();
            if (insideTexture != null) {
                renderer.setTexture(insideTexture);
                renderer.blit((int) (circle.x - circle.radius / 4), (int) (circle.y - circle.radius / 4), (int) circle.radius / 2, (int) circle.radius / 2);
            }

            i += thickness;
        }
    }

    /**
     * Get Ellipse
     * Get ellipse from x, y, radius, delta-radius and delta-value.
     *
     * @param x the x-coordinate.
     * @param y the y-coordinate.
     * @param r the radius.
     * @param i the delta-radius.
     * @return the ellipse.
     */
    protected static Circle getCircle(float x, float y, float r, float i) {
        return new Circle(x, y, r - i * 2f);
    }

    @Nullable
    public World getWorld() {
        if (BubbleBlaster.getInstance() == null) return null;

        return BubbleBlaster.getInstance().world;
    }

    @Override
    public void render(Renderer renderer, int mouseX, int mouseY, float deltaTime) {
        var world = this.getWorld();
        if (world == null) return;
        if (world.shuttingDown) return;
        if (!world.isInitialized()) return;

        var hudOverride = this.getWorld().getGamemode().getHudOverride();
        var hud = hudOverride != null ? hudOverride : HudType.getCurrent();

//        renderer.enableNoise();

        this.profiler.section("Render BG", () -> this.renderBackground(renderer, world, hud));
        this.profiler.section("Render Entities", () -> this.renderEntities(renderer, world));

        var player = world.getPlayer();
        var blindness = player.getActiveEffect(StatusEffects.BLINDNESS);
        if (blindness != null) {
            var strength = blindness.getStrength();
            var alpha = Mth.clamp((strength + 1) * 0x10 + 0x60, 0, 255);
            renderer.fill(0, 0, this.game.getWidth(), this.game.getHeight(), Colors.rgba(0, 0, 0, alpha));
        }

        this.profiler.section("Render HUD", () -> hud.renderHudOverlay(renderer, world, world.getGamemode(), deltaTime));

        this.renderConeLight(renderer, world);

//        renderer.disableNoise();
    }

    private void renderConeLight(Renderer renderer, World world) {
        var player = world.getPlayer();
        if (player == null || player.isBlind()) return;

        var x = player.getVisualX();
        var y = player.getVisualY();
        var direction = player.getRotation();

        this.coneLight.setPosition(x, y);
        this.coneLight.setDirection(direction);

        // End the sprite batch before rendering lights
        renderer.flush();

        // Set up OpenGL state for the ray handler
        Gdx.gl20.glEnable(GL20.GL_BLEND);
        Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl20.glDisable(GL20.GL_DEPTH_TEST);

        if (WorldRenderer.isNight()) {
            this.rayHandler.setCombinedMatrix(renderer.getCamera().combined, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            this.rayHandler.updateAndRender();
        }

        // Restore OpenGL state
        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl20.glDisable(GL20.GL_BLEND);
    }

    private void renderBackground(Renderer renderer, World world, HudType hud) {
        var currentGameplayEvent = world.getActiveEvent();
        if (currentGameplayEvent != null) {
            currentGameplayEvent.renderBackground(this.getWorld(), renderer);
        } else {
            if (!hud.renderBackground(renderer, BG_TOP, BG_BOTTOM)) {
                if (WorldRenderer.isNight()) {
                    renderer.fillGradient(0, 0, this.game.getWidth(), this.game.getHeight(), BG_TOP_NIGHT, BG_BOTTOM_NIGHT);
                } else {
                    renderer.fillGradient(0, 0, this.game.getWidth(), this.game.getHeight(), BG_TOP, BG_BOTTOM);
                }
            }
        }
    }

    private static boolean isNight() {
        var calendar = Calendar.getInstance();
        var hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour >= 18 || hour <= 6;
    }

    private void renderEntities(Renderer renderer, World world) {
        for (var entity : world.getEntities()) {
            if (entity == null) continue;
            if (entity.isVisible()) {
                if (entity instanceof Player player) {
                    RenderEvents.RENDER_PLAYER_BEFORE.factory().onRenderPlayerBefore(player, renderer);
                }
                RenderEvents.RENDER_ENTITY_BEFORE.factory().onRenderEntityBefore(entity, renderer);
                entity.render(renderer);
                RenderEvents.RENDER_ENTITY_AFTER.factory().onRenderEntityAfter(entity, renderer);
                if (entity instanceof Player) {
                    var player = (Player) entity;
                    RenderEvents.RENDER_PLAYER_AFTER.factory().onRenderPlayerAfter(player, renderer);
                }
            }
            if (this.game.isCollisionShapesShown()) {
                var shape = entity.getShape();
                renderer.setLineThickness(6.0f);
                renderer.outline(shape, Colors.BLACK);
                renderer.setLineThickness(2.0f);
                renderer.outline(shape, Colors.WHITE);
            }
        }
    }
}
