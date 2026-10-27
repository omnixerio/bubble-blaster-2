/// //////////////////
//     Package     //
/// //////////////////
package dev.ultreon.bubbles.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.FilmGrainEffect;
import com.crashinvaders.vfx.effects.GaussianBlurEffect;
import com.crashinvaders.vfx.effects.NfaaEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxFrameBufferPool;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import dev.ultreon.bubbles.Axis2D;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.BubbleBlasterConfig;
import dev.ultreon.bubbles.config.Config;
import dev.ultreon.bubbles.random.JavaRandom;
import dev.ultreon.bubbles.render.gui.border.Border;
import dev.ultreon.bubbles.util.StringUtils;
import dev.ultreon.libs.commons.v0.Anchor;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.libs.commons.v0.util.IOUtils;
import dev.ultreon.bubbles.text.TextObject;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import space.earlygrey.shapedrawer.DefaultSideEstimator;
import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

/**
 * Renderer class.
 *
 * @author XyperCode
 * @see FontRenderContext
 * @see Colors
 * @see String
 * @see ImageObserver
 * @see AffineTransform
 * @see AttributedCharacterIterator
 * @see GlyphVector
 * @see Polygon
 * @see Rectangle
 */
@SuppressWarnings({"FieldCanBeLocal"})
public class Renderer {
    private static final int MAX_BUBBLE_COLORS = 8;
    private static final Color ANIM_COLOR_1 = new Color(0f, 0.5f, 1f, 1f);
    private static final Color ANIM_COLOR_2 = new Color(0f, 1f, 0.5f, 1f);
    private static final Color ANIM_ERROR_COLOR_1 = Colors.rgb(0xff3000);
    private static final Color ANIM_ERROR_COLOR_2 = Colors.rgb(0xffa000);
    private final BubbleBlaster game = BubbleBlaster.getInstance();
    private final GL20 gl20;
    private final SpriteBatch batch;
    private final Set<BitmapFont> filteredFonts = Collections.newSetFromMap(new IdentityHashMap<>());
    private final ShapeDrawer shapes;
    private final OrthographicCamera camera;
    private final DefaultSideEstimator sides;
    private final VfxManager vfxManager;
    private final GaussianBlurEffect vfxBlur;
    private float lineThickness = 0.1f;
    private Texture texture;
    private BitmapFont font;
    private final ThreadLocal<GlyphLayout> glyphLayout = new ThreadLocal<>();
    private final Color clearColor = Colors.BLACK;
    private Color color;
    private boolean rendering;
    private volatile boolean triggerScissorLog = false; // TODO: DEBUG
    private boolean triggeredScissorLog = false; // TODO: DEBUG
    private boolean loggingScissors = false; // TODO: DEBUG
    private int scissorDepth = 0;
    private boolean blendingEnabled = true;
    private final Stack<VfxFrameBuffer> fboStack = new Stack<>();
    private final FboPool fboPool;
    private boolean depthEnabled;
    private boolean blurring;
    private final GlyphLayout layout = new GlyphLayout();
    private boolean hovering;
    private boolean hideCursor;
    private final List<Disposable> toDispose = new ArrayList<>();
    private final FilmGrainEffect vfxNoise;
    private boolean noising;
    private FrameBuffer grid;
    private int width;
    private int height;
    private final ShaderProgram gridShader;
    private final ShaderProgram blurShader;
    private final ShaderProgram gradientTextureShader;
    private final ShaderProgram gradientColorShader;
    private final ShaderProgram bubbleShader;
    private final TextureRegion white;
    private boolean blurred;
    private final long startTime = System.currentTimeMillis();

    @ApiStatus.Internal
    public Renderer(ShapeRenderer shapes, SpriteBatch batch, OrthographicCamera camera) {
        this.font = this.game.getBitmapFont();
        this.gl20 = Gdx.gl20;
        this.batch = batch;
        this.camera = camera;
        this.sides = new DefaultSideEstimator(20, 4000, 3600f);

        // Visual Effects setup.
        this.vfxManager = new VfxManager(Pixmap.Format.RGBA8888);
        this.vfxManager.setBlendingEnabled(true);
        this.vfxBlur = new GaussianBlurEffect(GaussianBlurEffect.BlurType.Gaussian5x5);
        this.vfxBlur.setPasses(15);
//        this.vfxBlur.setAmount(15);
        new NfaaEffect(true);
        this.vfxNoise = new FilmGrainEffect();
        this.vfxNoise.setNoiseAmount(0.25f);

        this.fboPool = new FboPool(Pixmap.Format.RGBA8888, this.getWidth(), this.getHeight(), 10);
        this.fboPool.setTextureParams(TextureWrap.Repeat, TextureWrap.Repeat, TextureFilter.Linear, TextureFilter.Linear);

        this.gridShader = new ShaderProgram(this.VERT, this.GRID_FRAG);
        this.blurShader = new ShaderProgram(this.VERT, this.FRAG);
        if (!this.gridShader.isCompiled())
            throw new GdxRuntimeException("Failed to compile grid shader");
        if (!this.blurShader.isCompiled())
            throw new GdxRuntimeException("Failed to compile blur shader");

        try {
            try (var vert = Renderer.class.getResourceAsStream("/assets/bubbleblaster/shaders/gradient.vert")) {
                if (vert == null)
                    throw new GdxRuntimeException("Failed to load gradient shader");
                try (var frag = Renderer.class.getResourceAsStream("/assets/bubbleblaster/shaders/gradient.frag")) {
                    if (frag == null)
                        throw new GdxRuntimeException("Failed to load gradient shader");
                    this.gradientTextureShader = new ShaderProgram(
                            new String(IOUtils.readAllBytes(vert)),
                            new String(IOUtils.readAllBytes(frag))
                    );
                }
            }

            try (var vert = Renderer.class.getResourceAsStream("/assets/bubbleblaster/shaders/gradient_color.vert")) {
                if (vert == null)
                    throw new GdxRuntimeException("Failed to load gradient shader");
                try (var frag = Renderer.class.getResourceAsStream("/assets/bubbleblaster/shaders/gradient_color.frag")) {
                    if (frag == null)
                        throw new GdxRuntimeException("Failed to load gradient shader");
                    this.gradientColorShader = new ShaderProgram(
                            new String(IOUtils.readAllBytes(vert)),
                            new String(IOUtils.readAllBytes(frag))
                    );
                }
            }
        } catch (IOException e) {
            throw new GdxRuntimeException("Failed to load gradient shader", e);
        }

        if (!this.gradientTextureShader.isCompiled()) {
            throw new GdxRuntimeException("Failed to compile gradient shader: " + this.gradientTextureShader.getLog());
        }

        if (!this.gradientTextureShader.getLog().isEmpty()) {
            BubbleBlaster.getLogger().warn("Failed to compile gradient shader: {}", this.gradientTextureShader.getLog());
        }

        this.gradientTextureShader.setUniformi("u_texture", 0);

        var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Colors.WHITE);
        pixmap.fill();
        var texture1 = new Texture(pixmap);
        this.white = new TextureRegion(texture1);

        this.bubbleShader = new ShaderProgram(
                "attribute vec4 a_position;\n" +
                        "attribute vec4 a_color;\n" +
                        "attribute vec2 a_texCoord0;\n" +
                        "uniform mat4 u_projTrans;\n" +
                        "uniform vec2 u_center;\n" +
                        "varying vec4 v_color;\n" +
                        "varying vec2 v_texCoords;\n" +
                        "varying vec2 v_localPosition;\n" +
                        "void main() { v_color = a_color; v_texCoords = a_texCoord0; v_localPosition = a_position.xy - u_center; gl_Position = u_projTrans * a_position; }",
                "#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
                        "varying vec4 v_color;\n" +
                        "varying vec2 v_texCoords;\n" +
                        "varying vec2 v_localPosition;\n" +
                        "uniform sampler2D u_texture;\n" +
                        "uniform float u_outerRadius;\n" +
                        "uniform float u_spacing;\n" +
                        "uniform int u_colorCount;\n" +
                        "uniform vec4 u_colors[8];\n" +
                        "void main() {\n" +
                        "  float distanceFromCenter = length(v_localPosition);\n" +
                        "  float spacing = max(u_spacing, 0.001);\n" +
                        "  float colorPosition = (u_outerRadius - distanceFromCenter) / spacing;\n" +
                        "  float lastPosition = float(u_colorCount - 1);\n" +
                        "  float clampedPosition = clamp(colorPosition, 0.0, lastPosition);\n" +
                        "  int colorIndex = int(floor(clampedPosition));\n" +
                        "  int nextColorIndex = min(colorIndex + 1, u_colorCount - 1);\n" +
                        "  vec4 firstColor = vec4(0.0);\n" +
                        "  vec4 nextColor = vec4(0.0);\n" +
                        "  for (int i = 0; i < 8; i++) {\n" +
                        "    if (i < u_colorCount) {\n" +
                        "      if (i == colorIndex) firstColor = u_colors[i];\n" +
                        "      if (i == nextColorIndex) nextColor = u_colors[i];\n" +
                        "    }\n" +
                        "  }\n" +
                        "  vec4 result = mix(firstColor, nextColor, fract(clampedPosition));\n" +
                        "  float feather = 0.75 / spacing;\n" +
                        "  float coverage = smoothstep(-feather, 0.0, colorPosition) * (1.0 - smoothstep(lastPosition + 0.5, lastPosition + 0.5 + feather, colorPosition));\n" +
                        "  result.a *= coverage;\n" +
                        "  gl_FragColor = result * v_color * texture2D(u_texture, v_texCoords).a;\n" +
                        "}");
        if (!this.bubbleShader.isCompiled()) {
            throw new GdxRuntimeException("Failed to compile bubble shader: " + this.bubbleShader.getLog());
        }

        this.shapes = new ShapeDrawer(batch, this.white);
    }

    /** Draws all concentric bubble outlines with one SpriteBatch draw call. */
    public boolean drawBubble(float x, float y, float radius, float spacing, float lineWidth, List<Color> colors, boolean night) {
        if (!this.rendering || colors.isEmpty() || colors.size() > MAX_BUBBLE_COLORS || radius <= 0) return false;

        var colorValues = new float[colors.size() * 4];
        for (var i = 0; i < colors.size(); i++) {
            var color = colors.get(i);
            var offset = i * 4;
            var nightMul = night ? 0.5f : 1.0f;
            colorValues[offset] = color.r * nightMul;
            colorValues[offset + 1] = color.g * nightMul;
            colorValues[offset + 2] = color.b * nightMul;
            colorValues[offset + 3] = color.a;
        }

        var previousShader = this.batch.getShader();
        this.batch.setShader(this.bubbleShader);
        var quadSize = radius + lineWidth;
        this.bubbleShader.setUniformf("u_center", x, y);
        this.bubbleShader.setUniformf("u_outerRadius", radius * 0.5f);
        this.bubbleShader.setUniformf("u_spacing", spacing);
        this.bubbleShader.setUniformi("u_colorCount", colors.size());
        this.bubbleShader.setUniform4fv("u_colors[0]", colorValues, 0, colorValues.length);
        this.batch.draw(this.white, x - quadSize * 0.5f, y - quadSize * 0.5f, quadSize, quadSize);
        this.batch.setShader(previousShader);
        return true;
    }

    @ApiStatus.Internal
    public void begin() {
        if (this.rendering)
            throw new IllegalStateException("Renderer is already rendering");

        this.shapes.setDefaultLineWidth(this.lineThickness);

        this.toDispose.forEach(Disposable::dispose);
        this.toDispose.clear();

        this.hovering = false;
        this.hideCursor = false;

        if (this.triggerScissorLog) {
            this.triggerScissorLog = false;
            this.triggeredScissorLog = true;
            this.loggingScissors = true;
        }

        this.batch.begin();

        this.rendering = true;

        this.clear();
        this.enableBlend();
        this.enableDepthTest();

        this.shapes.setSideEstimator(new DefaultSideEstimator(40, 4000, 4f));
    }

    @ApiStatus.Internal
    public void end() {
        if (!this.rendering)
            throw new IllegalStateException("Renderer isn't rendering yet");

        if (this.vfxManager.isCapturing())
            throw new IllegalStateException("Can´t end renderer while " + this.vfxManager.getClass().getSimpleName() + " is still capturing.");

        if (this.vfxManager.isApplyingEffects())
            throw new IllegalStateException("Can´t end renderer while " + this.vfxManager.getClass().getSimpleName() + " is still applying effects.");

        if (this.blurring)
            throw new IllegalStateException("Can´t end renderer while blurring mode is still enabled.");

        if (VfxFrameBuffer.getBufferNesting() != 0)
            throw new IllegalStateException("Renderer hasn't ended with FBO nesting cleared.");

        this.fboPool.freeAll();

        if (this.batch.isDrawing()) {
            this.batch.flush();
            this.batch.end();
        }

        this.disableDepthTest();
        this.disableBlend();

        if (this.triggeredScissorLog) {
            this.triggeredScissorLog = false;
            this.loggingScissors = false;
        }

        if (this.hideCursor) {
            Gdx.input.setCursorCatched(true);
        } else {
            Gdx.input.setCursorCatched(false);
            Gdx.graphics.setCursor(this.hovering ? this.game.handCursor : this.game.arrowCursor);
        }

        this.rendering = false;
    }

    public void enableBlend() {
        this.batch.enableBlending();
        this.batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        this.blendingEnabled = true;

        Gdx.gl20.glEnable(GL20.GL_BLEND);
        Gdx.gl20.glBlendFuncSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public boolean isBlendingEnabled() {
        return this.blendingEnabled;
    }

    private void enableDepthTest() {
        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl20.glDepthFunc(GL20.GL_LEQUAL);
    }

    public void disableBlend() {
        this.blendingEnabled = false;
        this.batch.disableBlending();

        Gdx.gl20.glDisable(GL20.GL_BLEND);
    }

    public void disableDepthTest() {
        Gdx.gl20.glDisable(GL20.GL_DEPTH_TEST);
    }

    public void outline(Shape2D shape, Color color) {
        if (shape instanceof Circle) {
            var circle = (Circle) shape;
            this.outline(circle, color);
        } else if (shape instanceof Rectangle) {
            var rectangle = (Rectangle) shape;
            this.outline(rectangle, color);
        } else if (shape instanceof Polygon) {
            var polygon = (Polygon) shape;
            this.outline(polygon, color);
        } else if (shape instanceof Ellipse) {
            var ellipse = (Ellipse) shape;
            this.outline(ellipse, color);
        } else throw new UnsupportedOperationException("Shape not supported: " + shape.getClass().getName());
    }

    public void outline(Rectangle rect, Color color) {
        if (!this.rendering) return;

        this.box(rect.x, rect.y, rect.width, rect.height, color);
    }

    public void outline(Ellipse ellipse, Color color) {
        if (!this.rendering) return;

        this.ellipse(ellipse.x, ellipse.y, ellipse.width, ellipse.height, color);
    }

    public void outline(Circle ellipse, Color color) {
        if (!this.rendering) return;

        this.circle(ellipse.x, ellipse.y, ellipse.radius, color);
    }

    public void fillCircle(float x, float y, float size, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.filledCircle(x, y, size / 2f);
    }

    public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.triangle(x1, y1, x2, y2, x3, y3);
    }

    public void triangle(float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.triangle(x1, y1, x2, y2, x3, y3);
    }

    public void circle(float x, float y, float size, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.circle(x, y, size / 2f, this.lineThickness, JoinType.SMOOTH);
    }

    public void fill(Shape2D s, Color color) {
        if (!this.rendering) return;

        if (s instanceof Circle) {
            var circle = (Circle) s;
            this.fill(circle, color);
        } else if (s instanceof Ellipse) {
            var ellipse = (Ellipse) s;
            this.fill(ellipse, color);
        } else if (s instanceof Rectangle) {
            var rect = (Rectangle) s;
            this.fill(rect, color);
        } else if (s instanceof Polygon) {
            var rect = (Polygon) s;
            this.fill(rect, color);
        } else if (s instanceof Polyline) {
            var rect = (Polyline) s;
            this.fill(rect, color);
        }
    }

    @ApiStatus.Experimental
    public boolean enableBlur(int radius) {
        if (this.blurring) {
            throw new IllegalStateException("Can't enable blur while already enabled!");
        }

        this.batch.flush();
        this.batch.end();

        this.vfxManager.cleanUpBuffers(Colors.BLACK);
        this.fboStack.push(this.beginCapture());
        var b = this.pushScissor(0, 0, this.getWidth(), this.getHeight());
        if (!b) {
            return false;
        }
        this.blurring = true;

        this.batch.begin();
        return true;
    }

    @ApiStatus.Experimental
    public void disableBlur() {
        this.batch.end();

        this.blurring = false;
        this.popScissor();

        // Add blur effect.
        this.vfxManager.addEffect(this.vfxBlur);

        this.vfxManager.useAsInput(this.endCapture(this.fboStack.pop()));

        this.batch.begin();

        // Apply the effects chain to the captured frame.
        // In our case, only one effect (gaussian blur) will be applied.
        this.vfxManager.applyEffects();

        // Render result to the screen.
        this.vfxManager.renderToScreen();

        // Add blur effect.
        this.vfxManager.removeAllEffects();
    }

    @ApiStatus.Experimental
    public boolean enableNoise() {
        if (this.noising) {
            throw new IllegalStateException("Can't enable blur while already enabled!");
        }

        this.batch.flush();
        this.batch.end();
        this.vfxNoise.setSeed(new JavaRandom().nextFloat(-Float.MAX_VALUE, Float.MAX_VALUE));

        this.vfxManager.cleanUpBuffers(Colors.BLACK);
        this.fboStack.push(this.beginCapture());
        var b = this.pushScissor(0, 0, this.getWidth(), this.getHeight());
        if (!b) return false;
        this.noising = true;

        this.batch.begin();
        return true;
    }

    @ApiStatus.Experimental
    public void disableNoise() {
        this.batch.end();

        this.noising = false;
        this.popScissor();

        // Add blur effect.
        this.vfxManager.addEffect(this.vfxNoise);

        this.vfxManager.useAsInput(this.endCapture(this.fboStack.pop()));

        this.batch.begin();

        // Apply the effects chain to the captured frame.
        // In our case, only one effect (gaussian blur) will be applied.
        this.vfxManager.applyEffects();

        // Render result to the screen.
        this.vfxManager.renderToScreen();

        // Add blur effect.
        this.vfxManager.removeAllEffects();
    }

    @ApiStatus.Experimental
    private void blur(VfxFrameBuffer fbo, int x, int y, int width, int height) {

        // Add blur effect.
        this.vfxManager.addEffect(this.vfxBlur);
        this.vfxManager.useAsInput(fbo);

        // Apply the effects chain to the captured frame.
        // In our case, only one effect (gaussian blur) will be applied.
        this.vfxManager.applyEffects();

        // Render result to the screen.
        this.flush();
        this.scissored(x, y, width, height, this.vfxManager::renderToScreen);

        // Add blur effect.
        this.vfxManager.removeEffect(this.vfxBlur);
    }

    @CheckReturnValue
    @ApiStatus.Experimental
    public VfxFrameBuffer beginCapture() {
        var fbo = this.fboPool.obtain();
        fbo.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
        return fbo;
    }

    @CanIgnoreReturnValue
    @ApiStatus.Experimental
    public VfxFrameBuffer endCapture(VfxFrameBuffer fbo) {
        fbo.end();
        return fbo;
    }

    @CheckReturnValue
    @ApiStatus.Experimental
    public VfxFrameBuffer capture(Runnable func) {
        var fbo = this.beginCapture();
        func.run();
        return this.endCapture(fbo);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;

        // Resize the sprite batch and shape renderer.
        this.batch.setProjectionMatrix(this.batch.getProjectionMatrix().setToOrtho(0, width, height, 0, 0, 1000000));

        // Resize the FBO pool.
        this.fboPool.resize(width, height);

        // VfxManager manages internal off-screen buffers,
        // which should always match the required viewport (whole screen in our case).
        this.vfxManager.resize(width, height);

        this.resizeGrid(width, height);
    }

    public void update(float deltaTime) {
        this.vfxManager.update(deltaTime);
    }

    public void withEffect(Runnable func) {
        this.withEffect(BubbleBlasterConfig.DEFAULT_EFFECT_SPEED.getOrDefault(), func);
    }

    public void withEffect(float speed, Runnable func) {
        this.withEffect(speed, ANIM_COLOR_1, ANIM_COLOR_2, func);
    }

    public void withEffect(Color color1, Color color2, Runnable func) {
        this.withEffect(BubbleBlasterConfig.DEFAULT_EFFECT_SPEED.getOrDefault(), color1, color2, func);
    }

    public void withEffect(float speed, Color color1, Color color2, Runnable func) {
        var shader = this.gradientTextureShader;
        this.batch.setShader(shader);
        this.batch.setColor(1f, 1f, 1f, 1f);
        this.shapes.setColor(1f, 1f, 1f, 1f);
        this.gradientTextureShader.setUniformf("u_time", (System.currentTimeMillis() - this.startTime) / 1000f);
        shader.setUniformf("u_resolution", Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        shader.setUniformf("u_speed", speed);
        shader.setUniformf("u_color1", color1);
        shader.setUniformf("u_color2", color2);
        func.run();
        this.batch.setShader(null);
    }

    private void disableStateChange() {
    }

    private void enableStateChange() {
    }

    public void fill(Circle ellipse, Color color) {
        if (!this.rendering) return;

        this.fillCircle(ellipse.x, ellipse.y, ellipse.radius, color);
    }

    public void fill(Ellipse ellipse, Color color) {
        if (!this.rendering) return;

        this.fillEllipse(ellipse.x, ellipse.y, ellipse.width, ellipse.height, color);
    }

    public void fill(Rectangle r, Color color) {
        if (!this.rendering) return;

        this.fill(r.getX(), r.getY(), r.getWidth(), r.getHeight(), color);
    }

    public void fill(Polygon polygon, Color color) {
        if (!this.rendering) return;

        this.fillPolygon(polygon, color);
    }

    public void fill(Polyline polyline, Color color) {
        if (!this.rendering) return;

        this.polyline(polyline, color);
    }

    public void fillGradient(Rectangle bounds, Color color1, Color color2) {
        if (!this.rendering) return;

        this.fillGradient(bounds.x, bounds.y, bounds.width, bounds.height, color1, color2, Axis2D.VERTICAL);
    }

    public void fillGradient(Rectangle bounds, Color color1, Color color2, Axis2D axis) {
        if (!this.rendering) return;

        this.fillGradient(bounds.x, bounds.y, bounds.width, bounds.height, color1, color2, axis);
    }

    public void fillGradient(float x, float y, float width, float height, Color color1, Color color2) {
        if (!this.rendering) return;

        this.fillGradient(x, y, width, height, color1, color2, Axis2D.VERTICAL);
    }

    public void fillGradient(float x, float y, float width, float height, Color color1, Color color2, Axis2D axis) {
        if (!this.rendering) return;

        switch (axis) {
            case HORIZONTAL:
                this.shapes.filledRectangle(x, y, width, height, color1, color2, color2, color1);
                break;
            case VERTICAL:
                this.shapes.filledRectangle(x, y, width, height, color2, color2, color1, color1);
                break;
        }
    }

    public void line(float x1, float y1, float x2, float y2, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.line(x1, y1, x2, y2);
    }

    public void line(float x1, float y1, float x2, float y2, Color color1, Color color2) {
        if (!this.rendering) return;

        this.shapes.line(x1, y1, x2, y2, color1, color2);
    }

    public void box(Rectangle bounds, Color color) {
        this.box(bounds, color, new Insets(1));
    }

    public void box(Rectangle bounds, Color color, Insets insets) {
        this.box(bounds.x, bounds.y, bounds.width, bounds.height, color, insets);
    }

    public void box(float x, float y, float width, float height, Color color) {
        if (!this.rendering) return;

        var border = new Border(1);
        border.setColor(color);
        border.drawBorder(this, x, y, width, height);
    }

    public void box(int x, int y, int width, int height, Color color, Insets insets) {
        if (!this.rendering) return;

        var border = new Border(insets);
        border.setColor(color);
        border.drawBorder(this, x, y, width, height);
    }

    public void box(float x, float y, float width, float height, Color color, Insets insets) {
        if (!this.rendering) return;

        var border = new Border(insets);
        border.setColor(color);
        border.drawBorder(this, (int) x, (int) y, (int) width, (int) height);
    }

    public void fill(float x, int y, float width, int height, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.filledRectangle(x, y, width, height);
    }

    public void fill(float x, float y, float width, float height, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.filledRectangle(x, y, width, height);
    }

    @ApiStatus.Experimental
    public void fillBlurred(VfxFrameBuffer fbo, int x, int y, int width, int height, Color color) {
        if (!this.rendering) return;

        this.blur(fbo, x, y, width, height);

        this.shapes.setColor(color);
        this.shapes.filledRectangle(x, y, width, height);
    }

    @Language("GLSL")
    final String VERT =
            "attribute vec4 a_position;\n" +
                    "attribute vec4 a_color;\n" +
                    "attribute vec2 a_texCoord0;\n" +
                    "uniform mat4 u_projTrans;\n" +
                    "\n" +
                    "varying vec4 vColor;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "\n" +
                    "void main() {\n" +
                    "	vColor = a_color;\n" +
                    "	vTexCoord = a_texCoord0;\n" +
                    "	gl_Position =  u_projTrans * a_position;\n" +
                    "}\n";

    @Language("GLSL")
    final String FRAG =
            "\n" +
                    "// Fragment shader\n" +
                    "#ifdef GL_ES\n" +
                    "precision mediump float;\n" +
                    "#endif\n" +
                    "\n" +
                    "varying vec4 vColor;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "\n" +
                    "uniform sampler2D u_texture;\n" +
                    "uniform vec2 iResolution;\n" +
                    "uniform float iBlurRadius; // Radius of the blur\n" +
                    "uniform vec2 iBlurDirection; // Direction of the blur\n" +
                    "\n" +
                    "void main() {\n" +
                    "  float Pi = 6.28318530718; // Pi*2\n" +
                    "\n" +
                    "  // GAUSSIAN BLUR SETTINGS {{{\n" +
                    "  float Directions = 16.0; // BLUR DIRECTIONS (Default 16.0 - More is better but slower)\n" +
                    "  float Quality = 4.0; // BLUR QUALITY (Default 4.0 - More is better but slower)\n" +
                    "  float Size = iBlurRadius; // BLUR SIZE (Radius)\n" +
                    "  // GAUSSIAN BLUR SETTINGS }}}\n" +
                    "\n" +
                    "  vec2 Radius = Size/iResolution.xy;\n" +
                    "\n" +
                    "  // Normalized pixel coordinates (from 0 to 1)\n" +
                    "  vec2 uv = gl_FragCoord.xy/iResolution.xy;\n" +
                    "  // Pixel colour\n" +
                    "  vec4 color = texture2D(u_texture, uv);\n" +
                    "\n" +
                    "  // Blur calculations\n" +
                    "  for( float d=0.0; d<Pi; d+=Pi/Directions)\n" +
                    "  {\n" +
                    "    for(float i=1.0/Quality; i<=1.0; i+=1.0/Quality)\n" +
                    "    {\n" +
                    "      color += texture2D(u_texture, uv+vec2(cos(d),sin(d))*Radius*i);\n" +
                    "    }\n" +
                    "  }\n" +
                    "\n" +
                    "  // Gamma correction\n" +
                    "  float Gamma = 1.01;\n" +
                    "  color.rgba = pow(color.rgba, vec4(1.0/Gamma));\n" +
                    "\n" +
                    "  // Output to screen\n" +
                    "  color /= Quality * Directions;\n" +
                    "  gl_FragColor = color;\n" +
                    "}\n";


    //    @Language("GLSL")
    final String GRID_FRAG =
            "varying vec2 vTexCoord;\n" +
                    "varying vec4 vColor;\n" +
                    "uniform sampler2D u_texture;\n" +
                    "uniform vec2 iResolution;\n" +
                    "uniform vec3 hexagonColor;\n" +
                    "uniform float hexagonTransparency;\n" +
                    "\n" +
                    "float rng( in vec2 pos )\n" +
                    "{\n" +
                    "    return fract(sin( pos.y + pos.x*78.233 )*43758.5453)*2.0 - 1.0;\n" +
                    "}\n" +
                    "\n" +
                    "float simplexValue1DPart(vec2 uv, float ix) {\n" +
                    "    float x = uv.x - ix;\n" +
                    "    float f = 1.0 - x * x;\n" +
                    "    float f2 = f * f;\n" +
                    "    float f3 = f * f2;\n" +
                    "    return f3;\n" +
                    "}\n" +
                    "\n" +
                    "float simplexValue1D(vec2 uv) {\n" +
                    "    vec2 iuv = floor(uv);    \n" +
                    "    float n = simplexValue1DPart(uv, iuv.x);\n" +
                    "    n += simplexValue1DPart(uv, iuv.x + 1.0);\n" +
                    "    return rng(vec2(n * 2.0 - 1.0, 0.0));\n" +
                    "}\n" +
                    "\n" +
                    "float perlin( in float pos )\n" +
                    "{\n" +
                    "    // Get node values\n" +
                    "    \n" +
                    "    float a = rng( vec2(floor(pos), 1.0) );\n" +
                    "    float b = rng( vec2(ceil( pos), 1.0) );\n" +
                    "    \n" +
                    "    float a_x = rng( vec2(floor(pos), 2.0) );\n" +
                    "    float b_x = rng( vec2(ceil( pos), 2.0) );\n" +
                    "    \n" +
                    "    a += a_x*fract(pos);\n" +
                    "    b += b_x*(fract(pos)-1.0);\n" +
                    "    \n" +
                    "    \n" +
                    "    \n" +
                    "    // Interpolate values\n" +
                    "    \n" +
                    "    return a + (b-a)*smoothstep(0.0,1.0,fract(pos));\n" +
                    "}\n" +
                    "\n" +
                    "void main() { \n" +
                    "  vec2 uv = gl_FragCoord.xy;\n" +
                    "  uv /= 24.0;\n" +
                    "\n" +
                    "  vec4 color = texture2D(u_texture, vTexCoord);\n" +
                    "  const float A = 0.0;\n" +
                    "  const float B = 0.15;\n" +
                    "\n" +
                    "  float x = uv.x;\n" +
                    "  float y = (uv.y) * (1.5 / 3.0);\n" +
                    "\n" +
                    "  float val = (0.5 + 0.5 * x + 0.5 * y);\n" +
                    "\n" +
                    "  float noise = perlin(val);\n" +
                    "  if (noise > 0.1) {\n" +
                    "      noise = -1.0;\n" +
                    "  }\n" +
                    "\n" +
                    "  noise = 1.0 - (noise + 1.0) / 2.0;\n" +
                    "\n" +
                    "  color.rgb = vec3(1.0);\n" +
                    "  color.a = color.a * (noise * (B - A)) + A;\n" +
                    "\n" +
                    "  gl_FragColor = color;\n" +
                    "}\n";

    @ApiStatus.Experimental
    public void blurred(Runnable block) {
        this.blurred(true, block);
    }

    @ApiStatus.Experimental
    public void blurred(float radius, Runnable block) {
        this.blurred(radius, true, block);
    }

    @ApiStatus.Experimental
    public void blurred(boolean grid, Runnable block) {
        this.blurred(grid, 1, block);
    }

    @ApiStatus.Experimental
    public void blurred(float radius, boolean grid, Runnable block) {
        this.blurred(radius, grid, 1, block);
    }

    @ApiStatus.Experimental
    public void blurred(boolean grid, int guiScale, Runnable block) {
        this.blurred(Config.blurRadius, grid, guiScale, block);
    }

    @ApiStatus.Experimental
    public void blurred(float radius, boolean grid, int guiScale, Runnable block) {
        this.blurred(1.0F, radius, grid, guiScale, block);
    }

    @ApiStatus.Experimental
    public void blurred(float overlayOpacity, float radius, boolean grid, int guiScale, Runnable block) {
        if (this.blurred) {
            block.run();
            return;
        }

        this.blurred = true;

        try {
            var backBufferWidth = Gdx.graphics.getBackBufferWidth();
            var backBufferHeight = Gdx.graphics.getBackBufferHeight();

            if (backBufferWidth <= 0 || backBufferHeight <= 0) {
                return;
            }

            var blurTargetA = new FrameBuffer(Pixmap.Format.RGBA8888, backBufferWidth, backBufferHeight, false);
            var blurTargetB = new FrameBuffer(Pixmap.Format.RGBA8888, backBufferWidth, backBufferHeight, false);
            var fboRegion = new TextureRegion(blurTargetA.getColorBufferTexture());

            // Start rendering to the default framebuffer
            this.batch.flush();

            //Start rendering to an offscreen color buffer
            blurTargetA.begin();

            //before rendering, ensure we are using the default shader
            this.batch.setShader(null);

            //render the batch contents to the offscreen buffer
            this.flush();

            block.run();

            //finish rendering to the offscreen buffer
            this.batch.flush();

            //finish rendering to the offscreen buffer
            blurTargetA.end();

            //now let's start blurring the offscreen image
            this.batch.setShader(this.blurShader);

            //since we never called batch.end(), we should still be drawing
            //which means are blurShader should now be in use

            // set the shader uniforms
            this.blurShader.setUniformf("iBlurDirection", 1f, 0f);
            this.blurShader.setUniformf("iResolution", backBufferWidth, backBufferHeight);
            this.blurShader.setUniformf("iBlurRadius", radius / guiScale);
            this.blurShader.setUniformf("iTime", System.currentTimeMillis() / 1000.0f);

            //our first blur pass goes to target B
            blurTargetB.begin();

            //we want to render FBO target A into target B
            fboRegion.setTexture(blurTargetA.getColorBufferTexture());

            //draw the scene to target B with a horizontal blur effect
            this.batch.setColor(1f, 1f, 1f, overlayOpacity);
            this.batch.draw(fboRegion, 0, 0, backBufferWidth, backBufferHeight);

            //flush the batch before ending the FBO
            this.batch.flush();

            //finish rendering target B
            blurTargetB.end();

            //now we can render to the screen using the vertical blur shader
            //update the blur only along Y-axis
            this.blurShader.setUniformf("iBlurDirection", 0f, 1f);

            //update the resolution of the blur along Y-axis
            this.blurShader.setUniformf("iResolution", backBufferWidth, backBufferHeight);

            //update the Y-axis blur radius
            this.blurShader.setUniformf("radius", radius);

            //draw target B to the screen with a vertical blur effect
            fboRegion.setTexture(blurTargetB.getColorBufferTexture());
            this.batch.setColor(1f, 1f, 1f, overlayOpacity);
            this.batch.draw(fboRegion, 0, 0);

            //reset to default shader without blurs
            this.flush();

            this.batch.setShader(null);

            //dispose of the FBOs
            blurTargetA.dispose();
            blurTargetB.dispose();
        } finally {
            this.blurred = false;
        }
    }

    public void blurred(Texture texture) {
        if (this.blurred) {
            return;
        }

        this.vfxManager.useAsInput(texture);
        this.vfxManager.applyEffects();
        this.vfxManager.renderToScreen(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());

        this.flush();
    }

    /** Captures the current framebuffer and applies the renderer's Gaussian blur shader off-screen. */
    public FrameBuffer captureBlurredFrame() {
        this.flush();
        this.batch.end();
        var capturedPixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Texture capturedTexture = null;
        FrameBuffer blurredFrame = null;
        var targetBound = false;
        var completed = false;
        try {
            capturedTexture = new Texture(capturedPixmap);
            capturedTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
            blurredFrame = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), false);
            blurredFrame.begin();
            targetBound = true;
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            this.batch.setColor(1, 1, 1, 1);
            this.batch.disableBlending();
            this.batch.begin();
            this.batch.setShader(this.blurShader);
            this.blurShader.setUniformf("iResolution", Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            this.blurShader.setUniformf("iBlurRadius", Config.blurRadius);
            this.batch.draw(capturedTexture, 0, 0, this.getWidth(), this.getHeight());
            this.batch.flush();
            this.batch.setShader(null);
            this.batch.end();
            blurredFrame.end();
            targetBound = false;
            completed = true;
            return blurredFrame;
        } finally {
            if (targetBound) blurredFrame.end();
            if (capturedTexture != null) capturedTexture.dispose();
            capturedPixmap.dispose();
            this.batch.begin();
            this.batch.enableBlending();
            this.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            if (!completed && blurredFrame != null) blurredFrame.dispose();
        }
    }

    /** Draws the part of a full-frame blurred image that lies under the given UI rectangle. */
    public void drawBlurredFrameRegion(Texture texture, float x, float y, float width, float height) {
        if (!this.rendering) return;

        var u = x / this.getWidth();
        var v = 1.0F - y / this.getHeight();
        var u2 = (x + width) / this.getWidth();
        var v2 = 1.0F - (y + height) / this.getHeight();
        this.batch.setColor(Color.WHITE);
        this.batch.draw(texture, x, y, width, height, u, v, u2, v2);
        this.flush();
    }

    public void resetGrid() {
        this.resizeGrid(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    private void resizeGrid(int width, int height) {
        if (width == 0 || height == 0) return;

        if (this.grid != null) this.grid.dispose();

        this.grid = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        this.grid.begin();
        Gdx.gl.glClearColor(1, 1, 1, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        try {
            var hexagonColorHex = Config.hexagonColor;
            if (hexagonColorHex == null) hexagonColorHex = "#ffffff";
            if (hexagonColorHex.length() > 7) hexagonColorHex = hexagonColorHex.substring(0, 7);
            if (hexagonColorHex.length() < 7 && hexagonColorHex.length() > 4)
                hexagonColorHex = hexagonColorHex.substring(0, 4);
            if (hexagonColorHex.length() < 4) hexagonColorHex = "#ffffff";
            Colors.hex(hexagonColorHex);
        } catch (dev.ultreon.bubbles.util.exceptions.InvalidValueException ignored) {
        }

        this.batch.begin();
        this.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        this.batch.setShader(this.gridShader);
        this.gridShader.setUniformf("iResolution", width, height);

        this.batch.draw(this.white, 0, 0, width, height);

        this.batch.setShader(null);
        this.batch.end();
        this.grid.end();
    }

    public void renderToScreen(VfxFrameBuffer fbo) {
        this.batch.draw(fbo.getTexture(), 0, 0, this.getWidth(), this.getHeight());
        this.flush();
    }

    public void arcLine(float x, float y, float radius, float startAngle, float angle, float lineWidth) {
        this.arcLine(x, y, radius, startAngle, angle, lineWidth, this.sides.estimateSidesRequired(1.0f, radius, radius));
    }

    public void arcLine(float x, float y, float radius, float startAngle, float angle, float lineWidth, int segments) {
        var endAngle = startAngle + angle;
        var vertices = new float[segments * 2];

        for (var i = 0; i < segments; i++) {
            var angleNow = MathUtils.lerp(startAngle, endAngle, (float) i / (segments - 1));
            var startX = x + radius * MathUtils.cosDeg(angleNow);
            var startY = y + radius * MathUtils.sinDeg(angleNow);

            vertices[i * 2] = startX;
            vertices[i * 2 + 1] = startY;
        }

        this.shapes.polygon(vertices);
    }

    public void roundRect(float x, float y, float width, float height, float radius) {
        if (!this.rendering) return;


        x += this.lineThickness / 2f;
        y += this.lineThickness / 2f;
        width -= this.lineThickness;
        height -= this.lineThickness;
        radius -= this.lineThickness / 2;

        var cornerRadius = Math.min(radius, Math.min(width, height) / 2 - 4);

        this.shapes.setDefaultLineWidth(this.lineThickness);
        this.shapes.line(x + cornerRadius, y, x + width - cornerRadius, y); // Draw top line
        this.shapes.line(x + cornerRadius, y + height, x + width - cornerRadius, y + height); // Draw bottom line
        this.shapes.line(x, y + cornerRadius, x, y + height - cornerRadius); // Draw left line
        this.shapes.line(x + width, y + cornerRadius, x + width, y + height - cornerRadius); // Draw right line

        this.arcLine(x + cornerRadius, y + cornerRadius, cornerRadius, 180f, 90f, this.lineThickness); // Draw top-left arc
        this.arcLine(x + width - cornerRadius, y + cornerRadius, cornerRadius, 270f, 90f, this.lineThickness); // Draw top-right arc
        this.arcLine(x + cornerRadius, y + height - cornerRadius, cornerRadius, 90f, 90f, this.lineThickness); // Draw bottom-left arc
        this.arcLine(x + width - cornerRadius, y + height - cornerRadius, cornerRadius, 0f, 90f, this.lineThickness); // Draw bottom-right arc
    }

    public void fillRoundRect(float x, float y, float width, float height, float radius, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);

        var cornerRadius = Math.min(radius, Math.min(width, height) / 2 - 4);

        // Draw the top-left rounded corner
        this.shapes.arc(x + cornerRadius, y + cornerRadius, cornerRadius, 180f, 91f, this.lineThickness);

        // Draw the top-right rounded corner
        this.shapes.arc(x + width - cornerRadius, y + cornerRadius, cornerRadius, 270f, 91f, this.lineThickness);

        // Draw the bottom-right rounded corner
        this.shapes.arc(x + width - cornerRadius, y + height - cornerRadius, cornerRadius, 0f, 91f, this.lineThickness);

        // Draw the bottom-left rounded corner
        this.shapes.arc(x + cornerRadius, y + height - cornerRadius, cornerRadius, 90f, 91f, this.lineThickness);

        // Draw the top and bottom straight sides
        this.shapes.filledRectangle(x + cornerRadius, y, width - 2 * cornerRadius, height);

        // Draw the left and right straight sides
        this.shapes.filledRectangle(x, y + cornerRadius, width, height - 2 * cornerRadius);

    }

    public void ellipse(int x, int y, int width, int height) {
        if (!this.rendering) return;

        this.shapes.setColor(this.color);
        this.shapes.ellipse(x, y, width, height);
    }

    public void ellipse(int x, int y, int width, int height, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.ellipse(x, y, width, height, 0f, this.lineThickness);
    }

    public void ellipse(float x, float y, float width, float height, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.ellipse(x, y, width, height, 0f, this.lineThickness);
    }

    public void fillEllipse(int x, int y, int width, int height) {
        if (!this.rendering) return;

        this.shapes.setColor(this.color);
        this.shapes.ellipse(x, y, width, height);
    }

    public void fillEllipse(int x, int y, int width, int height, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.ellipse(x, y, width, height);
    }

    public void fillEllipse(float x, float y, float width, float height, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.ellipse(x, y, width, height);
    }

    public void arcLine(int x, int y, int radius, int startAngle, int arcAngle, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.arc(x, y, radius, startAngle, arcAngle, this.lineThickness);
    }

    public void arc(int x, int y, int radius, int startAngle, int arcAngle, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.arc(x, y, radius, startAngle, arcAngle, this.lineThickness);
    }

    public void polygon(Polygon p, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.polygon(p.getTransformedVertices(), this.lineThickness, JoinType.SMOOTH);
    }

    public void fillPolygon(Polygon p, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.filledPolygon(p.getTransformedVertices());
    }

    public void fillPolyline(Polyline p) {
        if (!this.rendering) return;

        this.gl20.glLineWidth(this.lineThickness);
        this.shapes.setColor(this.color);
        this.shapes.polygon(p.getTransformedVertices());
    }

    public void polyline(Polyline p, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.polygon(p.getTransformedVertices(), this.lineThickness, JoinType.SMOOTH);
    }

    public void polyline(float[] vertices, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.polygon(vertices);
    }

    public void fillPolyline(Polyline p, Color color) {
        if (!this.rendering) return;

        this.shapes.setColor(color);
        this.shapes.filledPolygon(p.getTransformedVertices());
    }

    public void blit(Texture tex, float x, float y) {
        if (!this.rendering) return;

        this.batch.draw(tex, x, y + tex.getHeight(), tex.getWidth(), -tex.getHeight());
    }

    public void blit(Texture tex, float x, float y, Color backgroundColor) {
        if (!this.rendering) return;

        this.fill(x, y, tex.getWidth(), tex.getHeight(), backgroundColor);

        this.batch.draw(tex, x, y + tex.getHeight(), tex.getWidth(), -tex.getHeight());
    }

    public void blit(Texture tex, float x, float y, float width, float height) {
        if (!this.rendering) return;

        this.batch.draw(tex, x, y + height, width, -height);
    }

    public void blit(Texture tex, float x, float y, float width, float height, Color backgroundColor) {
        if (!this.rendering) return;

        this.fill(x, y, width, height, backgroundColor);

        this.batch.draw(tex, x, y + height, width, -height);
    }

    public void drawText(BitmapFont font, String str, int x, int y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        this.prepareFont(font);
        font.setColor(color);
        font.draw(this.batch, str, x, y);
    }

    public void drawText(BitmapFont font, String str, float x, float y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        this.prepareFont(font);
        font.setColor(color);
        font.draw(this.batch, str, x, y);
    }

    public void drawText(BitmapFont font, TextObject str, int x, int y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        this.prepareFont(font);
        font.setColor(color);
        font.draw(this.batch, str.getText(), x, y);
    }

    public void drawText(BitmapFont font, TextObject str, float x, float y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        this.prepareFont(font);
        font.setColor(color);
        font.draw(this.batch, str.getText(), x, y);
    }

    private static BitmapFont resolveFont(BitmapFont font) {
        return font instanceof dev.ultreon.bubbles.init.FontSet set ? set.get() : font;
    }

    private void prepareFont(BitmapFont font) {
        if (!this.filteredFonts.add(font)) return;

        for (var region : font.getRegions()) {
            region.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
        }
    }

    public void drawText(String str, float x, float y, Anchor anchor, Color color) {
        if (!this.rendering) return;

        this.prepareFont(this.font);

        var layout = this.glyphLayout.get();
        if (layout == null) {
            layout = new GlyphLayout();
            this.glyphLayout.set(layout);
        }

        layout.setText(this.font, str);

        this.font.setColor(color);
        this.font.draw(this.batch, str, x, y);
    }

    public void drawMultiLineText(BitmapFont font, String str, int x, int y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        for (var line : str.split("\n"))
            this.drawText(font, line, x, y += (int) font.getLineHeight(), color);
    }

    public void drawWrappedText(BitmapFont font, String text, int x, int y, int maxWidth, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        this.setColor(color);
        var lines = StringUtils.wrap(text, font, new GlyphLayout(), maxWidth);
        var joined = org.apache.commons.lang3.StringUtils.join(lines.toArray(new String[]{}), '\n');
        this.drawMultiLineText(font, joined, x, y, color);
    }

    public void drawWrappedText(BitmapFont font, TextObject text, int x, int y, int maxWidth, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        this.setColor(color);
        var lines = StringUtils.wrap(text.getText(), font, new GlyphLayout(), maxWidth);
        var joined = org.apache.commons.lang3.StringUtils.join(lines.toArray(new String[]{}), '\n');
        this.drawMultiLineText(font, joined, x, y, color);
    }

    public void drawTabbedText(BitmapFont font, String text, int x, int y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        for (var line : text.split("\t"))
            this.drawText(font, line, x += (int) font.getLineHeight(), y, color);
    }

    public void drawTextCenter(BitmapFont font, String text, float x, float y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        this.layout.setText(font, text);

        // Determine the X coordinate for the text
        x -= this.layout.width / 2;

        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top create the screen)
        y -= (this.layout.height + font.getDescent()) / 2;

        // Draw the String
        this.drawText(font, text, x, y, color);
    }

    public void drawTextCenter(BitmapFont font, TextObject text, float x, float y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        var string = text.getText();

        this.layout.setText(font, string);

        // Determine the X coordinate for the text
        x -= this.layout.width / 2;

        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top create the screen)
        y -= (this.layout.height + font.getDescent()) / 2;

        // Draw the String
        this.drawText(font, string, x, y, color);
    }

    public void drawTextLeft(BitmapFont font, String text, float x, float y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        this.layout.setText(font, text);

        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top create the screen)
        y -= (this.layout.height + font.getDescent()) / 2;

        // Draw the String
        this.drawText(font, text, x, y, color);
    }

    public void drawTextLeft(BitmapFont font, TextObject text, float x, float y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        var string = text.getText();

        this.layout.setText(font, string);

        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top create the screen)
        y -= (this.layout.height + font.getDescent()) / 2;

        // Draw the String
        this.drawText(font, string, x, y, color);
    }

    public void drawTextRight(BitmapFont font, String text, float x, float y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        this.layout.setText(font, text);

        // Determine the X coordinate for the text
        x -= this.layout.width;

        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top create the screen)
        y -= (this.layout.height + font.getDescent()) / 2;

        // Draw the String
        this.drawText(font, text, x, y, color);
    }

    public void drawTextRight(BitmapFont font, TextObject text, float x, float y, Color color) {
        if (!this.rendering) return;

        font = resolveFont(font);

        var string = text.getText();

        this.layout.setText(font, string);

        // Determine the X coordinate for the text
        x -= this.layout.width;

        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top create the screen)
        y -= (this.layout.height + font.getDescent()) / 2;

        // Draw the String
        this.drawText(font, string, x, y, color);
    }

    public void clear() {
        if (!this.rendering) return;

        // Clean up the screen.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Clean up internal buffers, as we don't need any information from the last render.
        this.vfxManager.cleanUpBuffers(Colors.BLACK);

        ScreenUtils.clear(this.clearColor, true);
    }

    @CheckReturnValue
    @ApiStatus.Internal
    private boolean pushScissor(Rectangle rect) {
        if (!this.rendering) return false;
        if (BubbleBlasterConfig.DEBUG_DISABLE_SCISSORS.get()) return true;

        this.flush();

//        rect.y = Gdx.graphics.getHeight() - rect.y - rect.height;

        return true;

//        if (!ScissorStack.pushScissors(rect)) {
//            if (this.loggingScissors) {
//                Debug.log("ScissorDebug", String.format("Scissor [%d]: %s", this.scissorDepth, rect));
//            }
//            return false;
//        }
//
//        if (this.loggingScissors) {
//            this.scissorDepth++;
//            Debug.log("ScissorDebug", String.format("Pushing scissor [%d]: %s", this.scissorDepth, rect));
//        }
//        return true;
    }

    public void flush() {
        this.batch.flush();
        Gdx.gl.glFlush();
    }

    public void triggerScissorLog() {
        this.triggerScissorLog = true;
    }

    @CheckReturnValue
    @ApiStatus.Internal
    private boolean pushScissor(float x, float y, float width, float height) {
        if (!this.rendering) return false;

        return this.pushScissor(new Rectangle(x, y, width, height));
    }

    @CheckReturnValue
    @ApiStatus.Internal
    private boolean pushScissor(int x, int y, int width, int height) {
        if (!this.rendering) return false;

        return this.pushScissor(new Rectangle(x, y, width, height));
    }

    @CanIgnoreReturnValue
    @ApiStatus.Internal
    private Rectangle popScissor() {
        if (!this.rendering) return null;

        if (BubbleBlasterConfig.DEBUG_DISABLE_SCISSORS.get()) return new Rectangle();

        if (!BubbleBlasterConfig.DEBUG_DISABLE_SCISSORS.getOrDefault()) {
            this.flush();
//            var rectangle = ScissorStack.popScissors();
//
//            if (this.loggingScissors) {
//                Debug.log("ScissorDebug", String.format("Popping scissor [%d]", this.scissorDepth));
//                this.scissorDepth--;
//            }
//            return rectangle;
        }
        return this.game.getBounds();
    }

    public void clearScissors() {
        if (!this.rendering) return;

        this.flush();
        while (ScissorStack.peekScissors() != null) {
            ScissorStack.popScissors();
        }
        this.flush();
    }

    public boolean hitClip(int x, int y, int width, int height) {
        return false;
    }

    public void drawEffectBox(Rectangle bounds) {
        if (!this.rendering) return;

        this.drawEffectBox(bounds.x, bounds.y, bounds.width, bounds.height, new Insets(2));
    }

    public void drawEffectBox(float x, float y, float width, float height) {
        if (!this.rendering) return;

        this.drawEffectBox(x, y, width, height, new Insets(2));
    }

    public void drawEffectBox(float x, float y, float width, float height, Insets insets) {
        if (!this.rendering) return;

        this.drawEffectBox(x, y, width, height, insets, BubbleBlasterConfig.DEFAULT_EFFECT_SPEED.getOrDefault());
    }

    public void drawEffectBox(Rectangle bounds, Insets insets) {
        if (!this.rendering) return;

        this.drawEffectBox(bounds.x, bounds.y, bounds.width, bounds.height, insets, BubbleBlasterConfig.DEFAULT_EFFECT_SPEED.getOrDefault());
    }

    public void drawEffectBox(float x, float y, float width, float height, Insets insets, float speed) {
        if (!this.rendering) return;

        var border = new Border(insets);
        border.setRenderType(Border.RenderType.EFFECT);
        border.setEffectSpeed(speed);
        border.drawBorder(this, x, y, width, height);
    }

    public void drawRoundEffectBox(float x, float y, float width, float height) {
        if (!this.rendering) return;

        this.drawRoundEffectBox(x, y, width, height, BubbleBlasterConfig.DEFAULT_EFFECT_SPEED.getOrDefault());
    }

    public void drawRoundEffectBox(float x, float y, float width, float height, float radius) {
        if (!this.rendering) return;

        this.drawRoundEffectBox(x, y, width, height, radius, 5);
    }

    public void drawRoundEffectBox(float x, float y, float width, float height, float radius, int borderWidth) {
        if (!this.rendering) return;

        this.drawRoundEffectBox(x, y, width, height, radius, borderWidth, BubbleBlasterConfig.DEFAULT_EFFECT_SPEED.getOrDefault());
    }

    public void drawRoundEffectBox(float x, float y, float width, float height, float radius, int lineWidth, float speed) {
        if (!this.rendering) return;

        this.setLineThickness(lineWidth);
        this.withEffect(() -> this.roundRect(x, y, width, height, radius));
    }

    public void drawErrorEffectBox(Rectangle bounds) {
        this.drawErrorEffectBox((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height);
    }

    public void drawErrorEffectBox(int x, int y, int width, int height) {
        if (!this.rendering) return;

        this.drawErrorEffectBox(x, y, width, height, new Insets(2));
    }

    public void drawErrorEffectBox(Rectangle bounds, Insets insets) {
        this.drawErrorEffectBox((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height, insets);
    }

    public void drawErrorEffectBox(int x, int y, int width, int height, Insets insets) {
        if (!this.rendering) return;

        this.drawErrorEffectBox(x, y, width, height, insets, BubbleBlasterConfig.DEFAULT_EFFECT_SPEED.getOrDefault());
    }

    public void drawErrorEffectBox(Rectangle bounds, Insets insets, float speed) {
        this.drawErrorEffectBox((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height, insets, speed);
    }

    public void drawErrorEffectBox(int x, int y, int width, int height, Insets insets, float speed) {
        if (!this.rendering) return;

        var border = new Border(insets);
        border.setRenderType(Border.RenderType.ERROR_EFFECT);
        border.setEffectSpeed(speed);
        border.drawBorder(this, x, y, width, height);
    }

    public void fillErrorEffect(float x, float y, float width, float height) {
        if (!this.rendering) return;

        this.fillScrollingGradient(x, y, width, height, BubbleBlasterConfig.DEFAULT_EFFECT_SPEED.getOrDefault(), ANIM_ERROR_COLOR_1, ANIM_ERROR_COLOR_2);
    }

    public void fillErrorEffect(float x, float y, float width, float height, float speed) {
        if (!this.rendering) return;

        this.fillScrollingGradient(x, y, width, height, speed, ANIM_ERROR_COLOR_1, ANIM_ERROR_COLOR_2);
    }

    public void fillEffect(float x, float y, float width, float height) {
        if (!this.rendering) return;

        this.fillScrollingGradient(x, y, width, height, BubbleBlasterConfig.DEFAULT_EFFECT_SPEED.getOrDefault(), ANIM_COLOR_1, ANIM_COLOR_2);
    }

    public void drawEffectBox(Rectangle bounds, Insets insets, float speed) {
        this.drawEffectBox((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height, insets, speed);
    }

    public void fillEffect(float x, float y, float width, float height, float speed) {
        if (!this.rendering) return;

        this.fillScrollingGradient(x, y, width, height, speed, ANIM_COLOR_1, ANIM_COLOR_2);
    }

    private void fillScrollingGradient(float x, float y, float width, float height, float speed, Color color1, Color color2) {
        this.withEffect(color1, color2, () -> {
            this.shapes.filledRectangle(x, y, width, height);
        });
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public OrthographicCamera getCamera() {
        return this.camera;
    }

    public void blit(int x, int y) {
        if (!this.rendering) return;

        var tex = this.texture;

        this.batch.draw(tex, x, y + tex.getHeight(), tex.getWidth(), -tex.getHeight());
    }

    public void blit(int x, int y, int width, int height) {
        if (!this.rendering) return;

        this.batch.draw(this.texture, x, y + height, width, -height);
    }

    public void setTexture(Identifier texture) {
        if (!this.rendering) return;

        this.texture = this.game.getTextureManager().getTexture(texture);
    }

    @ApiStatus.Experimental
    public void setTexture(Texture texture) {
        if (!this.rendering) return;

        this.texture = texture;
    }

    ////////////////////////
    //     Properties     //
    public void setLineThickness(float lineThickness) {
        if (!this.rendering) return;

        this.lineThickness = lineThickness;
    }

    public float getLineThickness() {
        return this.lineThickness;
    }

    public void setColor(Color c) {
        if (!this.rendering) return;
        if (c == null) return;

        this.font.setColor(c);
        this.shapes.setColor(c);
    }

    public void setColor(int r, int g, int b) {
        if (!this.rendering) return;

        this.setColor(Colors.rgb(r, g, b));
    }

    public void setColor(float r, float g, float b) {
        if (!this.rendering) return;

        this.setColor(Colors.rgb(r, g, b));
    }

    public void setColor(int r, int g, int b, int a) {
        if (!this.rendering) return;

        this.setColor(Colors.rgba(r, g, b, a));
    }

    public void setColor(float r, float g, float b, float a) {
        if (!this.rendering) return;

        this.setColor(Colors.rgba(r, g, b, a));
    }

    public void setColor(int argb) {
        if (!this.rendering) return;

        this.setColor(Colors.argb(argb));
    }

    /**
     * Sets current color from a color hex.
     * Examples:
     * <code>
     * color("#f70")
     * color("#fff7")
     * color("#ffd500")
     * color("#aab70077")
     * </code>
     *
     * @param hex a color hex.
     */
    public void setColor(String hex) {
        if (!this.rendering) return;

        this.setColor(Colors.hex(hex));
    }

    public Color getShapesColor() {
        return Colors.gdx(this.batch.getColor());
    }

    public void setClearColor(Color color) {
        if (!this.rendering) return;

        this.gl20.glClearColor(color.r, color.g, color.b, color.a);
    }

    public void setClearColor(int red, int green, int blue) {
        if (!this.rendering) return;

        this.setClearColor(Colors.rgb(red, green, blue));
    }

    public void setClearColor(float red, float green, float blue) {
        if (!this.rendering) return;

        this.setClearColor(Colors.rgb(red, green, blue));
    }

    public void setClearColor(int red, int green, int blue, int alpha) {
        if (!this.rendering) return;

        this.setClearColor(Colors.rgba(red, green, blue, alpha));
    }

    public void setClearColor(float red, float green, float blue, float alpha) {
        if (!this.rendering) return;

        this.setClearColor(Colors.rgba(red, green, blue, alpha));
    }

    public void setClearColor(int argb) {
        if (!this.rendering) return;

        this.setClearColor(Colors.argb(argb));
    }

    public void setClearColor(String hex) {
        if (!this.rendering) return;

        this.setClearColor(Colors.hex(hex));
    }

    public Color getClearColor() {
        return this.clearColor;
    }

    public void setFont(BitmapFont font) {
        if (!this.rendering) return;

        this.font = font;
    }

    public BitmapFont getFont() {
        return this.font;
    }

    public void roundedLine(float x1, float y1, float x2, float y2) {
//        this.shapes.path(Array.with(new Vector2(x1, y1), new Vector2(x2, y2)), this.lineWidth, JoinType.SMOOTH, false);
    }

    @Override
    public String toString() {
        return "Renderer{" +
                "rendering=" + this.rendering +
                '}';
    }

    public void scissored(Rectangle rect, Runnable func) {
        if (this.pushScissor(rect)) {
            func.run();
            this.popScissor();
        }
    }

    public void scissored(float x, float y, float width, float height, Runnable func) {
        if (this.pushScissor(x, y, width, height)) {
            func.run();
            this.popScissor();
        }
    }

    public void scissored(int x, int y, int width, int height, Runnable func) {
        if (this.pushScissor(x, y, width, height)) {
            func.run();
            this.popScissor();
        }
    }

    public void scissored(Rectangle rect, Consumer<Rectangle> func) {
        if (this.pushScissor(rect)) {
            func.accept(ScissorStack.peekScissors());
            this.popScissor();
        }
    }

    public void scissored(float x, float y, float width, float height, Consumer<Rectangle> func) {
        if (this.pushScissor(x, y, width, height)) {
            func.accept(ScissorStack.peekScissors());
            this.popScissor();
        }
    }

    public void scissored(int x, int y, int width, int height, Consumer<Rectangle> func) {
        if (this.pushScissor(x, y + height, width, height)) {
            func.accept(ScissorStack.peekScissors());
            this.popScissor();
        }
    }

    public void hovered() {
        this.hovering = true;
    }

    public void hideCursor() {
        this.hideCursor = true;
    }

    public int getFreeFrameBuffers() {
        return this.fboPool.getFreeCount();
    }

    public int getManagedFrameBuffers() {
        return this.fboPool.getManagedCount();
    }

    public enum State {
        BATCH, SHAPES
    }

    private class FboPool extends VfxFrameBufferPool {
        private final int capacity;

        public FboPool(Pixmap.Format format, int width, int height) {
            this(format, width, height, 16);
        }

        public FboPool(Pixmap.Format format, int width, int height, int capacity) {
            super(format, width, height, capacity);
            this.capacity = capacity;
        }

        @Override
        protected VfxFrameBuffer createBuffer() {
            if (this.managedBuffers.size >= this.getCapacity())
                throw new IllegalStateException("Frame buffer pool capacity reached: " + this.managedBuffers.size);
            return super.createBuffer();
        }

        public int getCapacity() {
            return this.capacity;
        }

        public int getManagedCount() {
            return this.managedBuffers.size;
        }

        public void freeAll() {
            this.cleanupInvalid();

            var copy = new Array<>(this.managedBuffers);
            for (var fbo : copy)
                this.free(fbo);

            copy.clear(); // Todo: Check performance.

            this.clearFree();
        }
    }
}
