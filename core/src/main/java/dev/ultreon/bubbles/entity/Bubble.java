package dev.ultreon.bubbles.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.BubbleBlasterConfig;
import dev.ultreon.bubbles.Constants;
import dev.ultreon.bubbles.bubble.BubbleType;
import dev.ultreon.bubbles.entity.ai.AiTask;
import dev.ultreon.bubbles.entity.attribute.Attribute;
import dev.ultreon.bubbles.entity.bubble.BubbleSystem;
import dev.ultreon.bubbles.entity.damage.EntityDamageSource;
import dev.ultreon.bubbles.entity.spawning.NaturalSpawnReason;
import dev.ultreon.bubbles.entity.spawning.SpawnInformation;
import dev.ultreon.bubbles.entity.spawning.SpawnUsage;
import dev.ultreon.bubbles.entity.types.EntityType;
import dev.ultreon.bubbles.gamemode.Gamemode;
import dev.ultreon.bubbles.gamemode.openworld.OpenWorldMode;
import dev.ultreon.bubbles.init.BubbleTypes;
import dev.ultreon.bubbles.init.Entities;
import dev.ultreon.bubbles.init.SoundEvents;
import dev.ultreon.bubbles.init.StatusEffects;
import dev.ultreon.bubbles.random.RandomSource;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.world.World;
import dev.ultreon.bubbles.world.WorldRenderer;
import dev.ultreon.ubo.types.MapType;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.libs.commons.v0.util.EnumUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Bubble Entity.
 * One create the most important parts create the game.
 *
 * @see AbstractBubbleEntity
 */
@SuppressWarnings({"SameParameterValue"})
public class Bubble extends AbstractBubbleEntity {
    // Rarities
    public static final int GIANT_BUBBLE_RARITY = 3000;

    // RNG IDs
    public static final int VARIANT_RNG = 1945857752;
    public static final int TYPE_RNG = -951578221;
    public static final int X_RNG_ID = 407004109;
    public static final int Y_RNG_ID = 1356169045;

    // General properties
    // Attributes
    protected float radius;
    protected int baseSize;
    protected float bounceAmount;
    protected float baseBounceAmount;

    // Bubble type.
    protected BubbleType bubbleType;

    // Entity type.
    private static final EntityType<Bubble> entityType = Entities.BUBBLE;

    private boolean effectApplied = false;
    private int destroyFrame;
    private boolean isBeingDestroyed;
    private Variant variant;

    // Light body for box2dLight shadow casting
    private Body lightBody;

    public static Variant getRandomVariant(World world, RandomSource random) {
        return random.nextInt(0, GIANT_BUBBLE_RARITY) == 0 ? Variant.GIANT : Variant.NORMAL;
    }

    public Bubble(World world) {
        this(world, Variant.NORMAL);
    }

    public Bubble(World world, Variant variant) {
        super(entityType, world);

        this.variant = variant;
        this.lightBody = null;
    }

    /**
     * Spawn Event Handler
     * On-spawn.
     *
     * @param information the entity's spawn data.
     */
    @Override
    public void preSpawn(SpawnInformation information) {
        var world = information.getWorld();
        var random = information.getRandom();

        super.preSpawn(information);

        if (information.getReason() instanceof NaturalSpawnReason) {
            var reason = (NaturalSpawnReason) information.getReason();
            var bubbleType = BubbleSystem.random(random, world);
            if (!bubbleType.canSpawn(information.getWorld())) {
                bubbleType = information.getWorld().getGamemode().getDefaultBubble();
            }
            this.bubbleType = bubbleType;

            // Get random properties
            var randomizer = world.getBubbleRandomizer();
            var properties = randomizer.randomProperties(world.game().getGameBounds(), random, reason.getRetry(), world, this);

            this.invincible = properties.getType().isInvincible();
            this.radius = properties.getRadius();
            this.bounceAmount = this.bubbleType.getBounceAmount();

            this.attributes.setBase(Attribute.MAX_HEALTH, properties.getDamageValue());
            this.attributes.setBase(Attribute.ATTACK, properties.getAttack());
            this.attributes.setBase(Attribute.DEFENSE, properties.getDefense());
            this.attributes.setBase(Attribute.SCORE, properties.getScoreMultiplier());
            this.attributes.setBase(Attribute.SPEED, properties.getSpeed() * Constants.BASE_BUBBLE_SPEED);

            this.health = properties.getDamageValue();

            // Set velocity
            this.velocity.y = 0;

            // Set attributes.
            this.attributes.setBase(Attribute.DEFENSE, 0.5f);

            this.rotation = 180;

            if (reason.getUsage() == SpawnUsage.BUBBLE_INIT_SPAWN) {
                this.pos.set(random.nextFloat(-this.radius, this.world.game().getWidth() + this.radius), random.nextFloat(-this.radius, this.world.game().getHeight() + this.radius));
            }
        }

        this.markAsAttackable(Entities.PLAYER);
        if (this.bounceAmount > 0) {
            this.markAsCollidable(Entities.BUBBLE);
        }
        this.markAsCollidable(Entities.BULLET);
        this.markAsCollidable(Entities.PLAYER);

        this.make();
        
        // Create light-blocking body for this bubble
        this.createLightBody();
    }

    @Override
    public AiTask nextAiTask() {
        if (this.currentAiTask != null && this.currentAiTask.executeTask(this)) {
            return this.currentAiTask;
        }
        for (var task : this.bubbleType.getAiTasks()) {
            if (task.executeTask(this)) {
                return this.currentAiTask = task;
            }
        }
        return this.currentAiTask = null;
    }

    @Override
    public Iterable<AiTask> getAiTasks() {
        return this.bubbleType.getAiTasks();
    }

    @Override
    public void onCollision(Entity other, double deltaTime) {
        if (other instanceof Bubble) {
            if (this.bounceAmount > 0) {
                other.bounceOff(this, this.bounceAmount, (float) deltaTime);
            }
            return;
        }

        this.bubbleType.onCollision(this, other);

        if (this.bounceAmount > 0) {
            // bounceAmount is an impulse strength. Do not scale it by the
            // per-tick delta: that made the configured bounce nearly vanish.
            other.bounceOff(this, this.bounceAmount, (float) deltaTime);
        }
    }

    /**
     * <p><b>Warning: </b><i>Unsafe method! Use {@link World#spawn(EntityType, SpawnInformation)} instead.</i></p>
     *
     * @see World#spawn(EntityType, SpawnInformation)
     * @see World#spawn(Entity, SpawnInformation)
     */
    @Override
    protected void make() {
        this.valid = true;
    }

    /**
     * <p><b>Warning: </b><i>Unsafe method! Use {@link #delete()}  removeBubble create GameType} instead.</i></p>
     *
     * @see #delete()
     */
    @Override
    protected void invalidate() {
        this.valid = false;
        
        // Destroy light-blocking body when bubble is deleted
        this.destroyLightBody();
    }

    /**
     * @return the bubble-entity's bounds.
     */
    @Override
    public Rectangle getBounds() {
        var circle = this.getShape();
        if (this.world.getGamemode() instanceof OpenWorldMode) {
            var rel = this.game.player.pos;
            return new Rectangle((float) (this.pos.x - circle.radius / 2 - rel.x), (float) (this.pos.y - circle.radius / 2 - rel.y), circle.radius, circle.radius);
        }
        var rectangle = new Rectangle((float) (this.pos.x - circle.radius / 2), (float) (this.pos.y - circle.radius / 2), circle.radius, circle.radius);
        rectangle.width += 4;
        rectangle.height += 4;
        return rectangle;
    }

    @Override
    public boolean isBad() {
        return this.bubbleType.isBad();
    }

    /**
     * Tick bubble entity.
     *
     * @param world the world where the entity is from.
     */
    @Override
    public void tick(World world) {
        var gamemode = this.world.getGamemode();
        var isOpenWorld = gamemode instanceof OpenWorldMode;

        // Check player and current scene.
        var player = this.world.getPlayer();

        this.prevPos.set(this.pos);

        if (this.isBeingDestroyed) {
            this.destroyFrame++;
            if (this.destroyFrame >= 10) {
                this.delete();
            }
        }

        super.tick(world);
        
        // Update light body position to match bubble position
        this.updateLightBody();

        if (!isOpenWorld && this.pos.x + this.radius < 0) {
            this.delete();
        }
    }

    @Override
    public boolean hasAi() {
        if (this.isBeingDestroyed) return false;
        return super.hasAi();
    }

    @Override
    public float getSpeed() {
        return super.getSpeed() * this.getLevelSpeedModifier() * this.getWorld().getGlobalBubbleSpeedModifier();
    }

    private float getLevelSpeedModifier() {
        if (Constants.DIFFICULTY_EFFECT_TYPE.isSpeed())
            return this.world.getPlayer().getLevel() / 2f + 1;

        return 1;
    }

    /**
     * Rendering method.<br>
     * <b>SHOULD NOT BE CALLED!</b>
     *
     * @param renderer renderer to render the bubble entity with.
     */
    @Override
    public void render(Renderer renderer) {
        if (this.willBeDeleted()) return;
//        renderer.image(TextureCollections.BUBBLE_TEXTURES.get().get(new TextureCollection.Index(getBubbleType().id().location(), getBubbleType().id().path() + "/" + radius)), (int) x - radius / 2, (int) y - radius / 2);
        var player = this.world.getPlayer();
        var x = this.pos.x;
        var y = this.pos.y;

        if (this.world.getGamemode() instanceof OpenWorldMode) {
            var rel = this.game.player.pos;
            x = rel.x - x + this.world.game().getWidth() / 2f;
            y = rel.y - y + this.world.game().getHeight() / 2f;
        }

        if (x + this.radius < 0 || x - this.radius > this.world.game().getWidth() || y + this.radius < 0 || y - this.radius > this.world.game().getHeight()) {
            return;
        }

        var renderType = player != null && player.getActiveEffect(StatusEffects.BLINDNESS) != null
                ? BubbleTypes.NORMAL
                : this.bubbleType;
        WorldRenderer.drawBubble(renderer, (float) x, (float) y, this.radius, this.destroyFrame, renderType);
    }

    public boolean isBeingDestroyed() {
        return this.isBeingDestroyed;
    }

    /**
     * @return the bubble's shape.
     */
    @Override
    public Circle getShape() {
        var rad = this.radius / 2;
        if (this.world.getGamemode() instanceof OpenWorldMode) {
            var rel = this.game.player.pos;
            return new Circle((float) (this.pos.x - rad), (float) (this.pos.y - rad - rel.y), rad);
        }
        return new Circle((float) (this.pos.x - rad), (float) (this.pos.y - rad), rad);
    }

    @Override
    public boolean isVisible() {
        if (this.world.getGamemode() instanceof OpenWorldMode) return true;
        var bounds = BubbleBlaster.getInstance().getBounds();
        return this.pos.x + this.radius >= 0 && this.pos.y + this.radius >= 0 &&
                this.pos.x - this.radius <= bounds.width && this.pos.y - this.radius <= bounds.height;
    }
    
    // Light body management methods
    
    private void createLightBody() {
        var lightWorld = WorldRenderer.getLightWorld();
        if (lightWorld == null) return;
        
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(this.getVisualX(), this.getVisualY());
        
        this.lightBody = lightWorld.createBody(bodyDef);
        
        CircleShape circleShape = new CircleShape();
        circleShape.setRadius(this.getRadius() / 2f);
        this.lightBody.createFixture(circleShape, 0);
        circleShape.dispose();
    }
    
    private void updateLightBody() {
        if (this.lightBody != null) {
            this.lightBody.setTransform(this.getVisualX(), this.getVisualY(), 0f);
        }
    }
    
    private void destroyLightBody() {
        if (this.lightBody != null) {
            var lightWorld = WorldRenderer.getLightWorld();
            if (lightWorld != null) {
                lightWorld.destroyBody(this.lightBody);
            }
            this.lightBody = null;
        }
    }
    
    public Body getLightBody() {
        return this.lightBody;
    }

    ///////////////////////////////////////////////////////////////////////////
    //     Attributes     //
    ////////////////////////

    // Radius.
    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public float getRadius() {
        return this.radius;
    }

    @Override
    public float radius() {
        return this.getShape().radius;
    }

    public int getBaseSize() {
        return this.baseSize;
    }

    // Bounce amount.
    public void setBounceAmount(float bounceAmount) {
        this.bounceAmount = bounceAmount;
    }

    public float getBounceAmount() {
        return this.bounceAmount;
    }

    public float getBaseBounceAmount() {
        return this.baseBounceAmount;
    }

    ///////////////////////////////////////////////////////////////////////////
    //     Bubble Type     //
    /////////////////////////
    public BubbleType getBubbleType() {
        return this.bubbleType;
    }

    public void setBubbleType(BubbleType type) {
        this.bubbleType = type;
    }

    @Override
    public void checkHealth() {

    }

    @Override
    public void setHealth(double hardness) {
        super.setHealth(hardness);
        this.radius = (int) hardness + 4;
        this.checkHealth();
    }

    @Override
    public void damage(double value, EntityDamageSource source) {
        if (this.isBeingDestroyed) return;
        super.damage(value / this.attributes.getBase(Attribute.DEFENSE), source);
        if (this.invincible) return;
        this.pop();
    }

    public void pop() {
        if (this.isValid() && this.isVisible())
            SoundEvents.BUBBLE_POP.play(0.3f);

        this.delete();
        this.isBeingDestroyed = true;
        this.attributes.removeModifiers(Attribute.SCORE);
        this.attributes.setBase(Attribute.SCORE, 0);
        this.canCollideWith.clear();
        this.canAttack.clear();
    }

    @Override
    public @NotNull MapType save() {
        @NotNull MapType data = super.save();
        data.putFloat("radius", this.radius);
        data.putInt("baseRadius", this.baseSize);

        data.putFloat("bounceAmount", this.bounceAmount);
        data.putFloat("baseBounceAmount", this.baseBounceAmount);

        data.putBoolean("effectApplied", this.effectApplied);
        var id = Registries.BUBBLES.getKey(this.bubbleType);
        data.putString("bubbleType", id == null ? BubbleTypes.NORMAL.getId().toString() : id.toString());

        return data;
    }

    @Override
    public void load(MapType data) {
        super.load(data);

        this.radius = data.getInt("radius");
        this.baseSize = data.getInt("baseRadius");

        this.bounceAmount = data.getFloat("bounceAmount");
        this.baseBounceAmount = data.getFloat("baseBounceAmount");
        this.variant = EnumUtils.byName(data.getString(this.variant.name(), ""), Variant.NORMAL);

        var bubbleTypeKey = Identifier.parse(data.getString("bubbleType"));
        this.effectApplied = data.getBoolean("effectApplied");
        this.bubbleType = Registries.BUBBLES.getValue(bubbleTypeKey);
    }

    public void setEffectApplied(boolean effectApplied) {
        this.effectApplied = effectApplied;
    }

    public boolean isEffectApplied() {
        return this.effectApplied;
    }

    public enum Variant {
        NORMAL,
        GIANT
    }
}
