package dev.ultreon.bubbles.entity.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.*;
import dev.ultreon.bubbles.*;
import dev.ultreon.bubbles.effect.BlindnessStatusEffect;
import dev.ultreon.bubbles.effect.StatusEffectInstance;
import dev.ultreon.bubbles.entity.Bubble;
import dev.ultreon.bubbles.entity.Bullet;
import dev.ultreon.bubbles.entity.Entity;
import dev.ultreon.bubbles.entity.LivingEntity;
import dev.ultreon.bubbles.entity.ammo.AmmoType;
import dev.ultreon.bubbles.entity.attribute.Attribute;
import dev.ultreon.bubbles.entity.damage.EntityDamageSource;
import dev.ultreon.bubbles.entity.player.ability.AbilityContainer;
import dev.ultreon.bubbles.entity.spawning.SpawnInformation;
import dev.ultreon.bubbles.event.v1.PlayerEvents;
import dev.ultreon.bubbles.gamemode.openworld.OpenWorldMode;
import dev.ultreon.bubbles.init.AmmoTypes;
import dev.ultreon.bubbles.init.Entities;
import dev.ultreon.bubbles.init.StatusEffects;
import dev.ultreon.bubbles.item.collection.PlayerItemCollection;
import dev.ultreon.bubbles.player.InputController;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.screen.CommandScreen;
import dev.ultreon.bubbles.util.TimeProcessor;
import dev.ultreon.bubbles.util.TimeUtils;
import dev.ultreon.bubbles.vector.Vector2D;
import dev.ultreon.bubbles.world.World;
import dev.ultreon.ubo.types.MapType;
import dev.ultreon.libs.commons.v0.Mth;
import dev.ultreon.libs.datetime.v0.Duration;
import org.apache.commons.lang3.SystemUtils;
import org.checkerframework.checker.guieffect.Effect;
import org.jetbrains.annotations.NotNull;

import static dev.ultreon.bubbles.BubbleBlaster.TPS;

/**
 * <p>These are the vertex coordinates:</p>
 * <br>
 * <pre>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|- Middle point<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br>
 * 9876543210123456789<br>
 * --------------------,<br>
 * ....#*********#.....| -8<br>
 * ....*.........*.....| -7<br>
 * ....#***#.#***#.....| -6<br>
 * ........*.*.........| -5<br>
 * ..#*****#.#******#..| -4<br>
 * .*..................| -3<br>
 * #..................#| -2<br>
 * *..................*| -1<br>
 * *..................*| 0  // Middle point.<br>
 * *..................*| 1<br>
 * #..................#| 2<br>
 * .*................*.| 3<br>
 * ..#..............#..| 4<br>
 * </pre>
 */
public class Player extends LivingEntity implements InputController {
    private static final float[] ARROW_VERTICES = {
            -5, 0,
            -10, -10,
            15, 0,
            -10, 10
    };
    private static final float RADIUS = 20;
    private static final float DRAG = 0.98f;

    private final Circle shipShape;
    private final Polygon arrowShape;
    private boolean joinInvincibility;
    private int invincibilityTicks;

    // Types
    private AmmoType currentAmmo = AmmoTypes.BASIC;

    // Motion (Arrow Keys).
    private float moving = 0f;
    private float rotating = 0f;

    /**
     * Amount of degrees to rotate in a second.
     */
    private float rotationSpeed = 120f;

    // Delta velocity.
    private float velocityDelta = 0f;

    // Normal values/
    private double score = 0.0;
    private int level = 1;

    // Modifiers.
    private long abilityEnergy;

    // Ability
    private final AbilityContainer abilityContainer = new AbilityContainer(this);
    private final PlayerItemCollection inventory = new PlayerItemCollection(this);
    private int shootCooldown;
    public final Vector2 tempVel = new Vector2();
    public long boostRefillTimer = TimeUtils.toTicks(Duration.ofMilliseconds(Constants.BOOST_COOLDOWN));
    public long boostAccelTimer = 0;
    private int successRate;
    private boolean brake;
    private double currentSpeed;

    /**
     * Player entity.
     * The player is controlled be the keyboard and is one create the important features create the game. (Almost any game).
     *
     * @see LivingEntity
     */
    public Player(World world) {
        super(Entities.PLAYER, world);

        this.markAsCollidable(Entities.BUBBLE);
        this.markAsCollidable(Entities.PLAYER);

        this.shipShape = new Circle(-20, -20, 40);
        this.arrowShape = new Polygon(ARROW_VERTICES);

        this.velocity.setZero();

        this.attributes.setBase(Attribute.DEFENSE, 1f);
        this.attributes.setBase(Attribute.ATTACK, 0.75f);
        this.attributes.setBase(Attribute.MAX_HEALTH, 30f);
        this.attributes.setBase(Attribute.SPEED, 4f);
        this.attributes.setBase(Attribute.SCORE_MODIFIER, 1f);

        this.health = 30f;

        for (var entityType : Registries.ENTITIES.values()) {
            this.markAsAttackable(entityType);
        }

        this.invincibilityTicks = TPS * 5;
        this.joinInvincibility = true;
        this.invincible = true;
    }

    /**
     * @param s the message.
     */
    public void sendSystemMessage(String s) {
        CommandScreen.addMessage(s, true);
    }

    /**
     * @param s the message.
     */
    public void sendChatMessage(String s) {
        LoadedGame.addMessage(s);
    }

    /**
     * Prepare spawn
     * Called when the entity was spawned.
     *
     * @param information the data to spawn with.
     */
    @Override
    public void preSpawn(SpawnInformation information) {
        super.preSpawn(information);
        var game = this.world.game();
        var gameBounds = game.getGameBounds();

        if (this.world.getGamemode() instanceof OpenWorldMode) {
            this.pos.set(0.0, -8000.0);
        } else {
            this.pos.x = Mth.clamp(this.pos.x, gameBounds.getX(), gameBounds.getX() + gameBounds.getWidth());
            this.pos.y = Mth.clamp(this.pos.y, gameBounds.getY(), gameBounds.getY() + gameBounds.getHeight());
        }
        this.make();
    }

    @Override
    protected void make() {
        this.valid = true;
    }

    @Override
    protected void invalidate() {
        this.valid = false;
    }

    @Override
    public Circle getShape() {
        if (this.world.getGamemode() instanceof OpenWorldMode) {
            return new Circle(this.world.game().getWidth() / 2f - RADIUS * 2 * this.scale / 2, this.world.game().getHeight() / 2f - RADIUS * 2 * this.scale / 2, RADIUS * 2 * this.scale);
        }
        return new Circle((float) (this.pos.x - RADIUS * 2 * this.scale / 2), (float) (this.pos.y - RADIUS * 2 * this.scale / 2), RADIUS * 2 * this.scale);
    }

    @Override
    public Rectangle getBounds() {
        if (this.world.getGamemode() instanceof OpenWorldMode)
            return new Rectangle(this.world.game().getWidth() / 2f - RADIUS, this.world.game().getHeight() / 2f - RADIUS, RADIUS * 2, RADIUS * 2);
        return new Rectangle((float) (this.pos.x - RADIUS), (float) (this.pos.y - RADIUS), RADIUS * 2, RADIUS * 2);
    }

    private Circle transformShip(Circle ship) {
        if (this.world.getGamemode() instanceof OpenWorldMode) {
            ship.setPosition(this.world.game().getWidth() / 2f, this.world.game().getHeight() / 2f);
            return ship;
        }
        ship.setPosition((float) this.pos.x, (float) this.pos.y);
        return ship;
    }

    private Polygon transformArrow(Polygon arrow) {
        if (this.world.getGamemode() instanceof OpenWorldMode) {
            arrow.setRotation(this.rotation + 180f);
            arrow.setPosition(this.world.game().getWidth() / 2f, this.world.game().getHeight() / 2f);
            return arrow;
        }
        // Set position based on mouse cursor
        arrow.setRotation(this.rotation);
        arrow.setPosition((float) this.pos.x, (float) this.pos.y);

        return arrow;
    }

    /**
     * @return the shape create the ship.
     */
    public Circle getShipShape() {
        return this.transformShip(this.shipShape);
    }

    /**
     * @return the arrow shape create the ship.
     */
    public Polygon getArrowShape() {
        return this.transformArrow(this.arrowShape);
    }

    /**
     * @return the center position.
     */
    public Vector2D getCenter() {
        return this.pos.cpy();
    }

    /**
     * Tick the player.
     *
     * @param world the game-type where the entity is from.
     */
    @Override
    public void tick(World world) {
        //***********************//
        // Spawn and load checks //
        //***********************//
        var loadedGame = BubbleBlaster.getInstance().getLoadedGame();

        if (loadedGame == null) {
            return;
        }

        if (this.isNotSpawned()) return;

        //**************************//
        // Player component ticking //
        //**************************//
        if (this.boostRefillTimer > 0) {
            this.boostRefillTimer--;
        }

        if (this.boostAccelTimer > 0) {
            this.boostAccelTimer--;
            this.accelerate(15f, false);
        } else if (this.boostAccelTimer == 0 && this.boostRefillTimer == -1) {
            this.boostRefillTimer = TimeUtils.toTicks(Duration.ofMilliseconds(Constants.BOOST_COOLDOWN));
        }

        this.abilityContainer.onEntityTick();
        this.inventory.tick();

        //****************//
        // Player motion. //
        //****************//
        var motion = 0.0f;
        var rotate = 0.0f;

        // Check each direction, to create velocity
        if (!(world.getGamemode() instanceof OpenWorldMode)) {
            this.moving = Mth.clamp(this.moving, -1, 1);
            this.rotating = Mth.clamp(this.rotating, -1, 1);
        }

        if (this.moving != 0 && this.canMove) motion += this.getSpeed() * this.moving;
        if (this.rotating != 0 && this.canMove) rotate += this.rotationSpeed * this.rotating;

        // Update X, and Y.
        if (this.canMove) {
            this.rotation += rotate / TPS;
        }

        // Calculate Velocity X and Y.
        var angelRadians = this.rotation * MathUtils.degRad;
        var tempVelX = MathUtils.cos(angelRadians) * motion;
        var tempVelY = MathUtils.sin(angelRadians) * motion;
        this.tempVel.set(tempVelX, tempVelY);

        if (this.canMove) {
            this.accel.add(this.tempVel);
        }

        // Velocity on X-axis.
        if (this.velocity.x > 0) {
            if (this.velocity.x + this.velocityDelta < 0) {
                this.velocity.x = 0;
            } else {
                this.velocity.x += this.velocityDelta;
            }
        } else if (this.velocity.x < 0) {
            if (this.velocity.x + this.velocityDelta > 0) {
                this.velocity.x = 0;
            } else {
                this.velocity.x -= this.velocityDelta;
            }
        }

        // Velocity on Y-axis.
        if (this.velocity.y > 0) {
            if (this.velocity.y + this.velocityDelta < 0) {
                this.velocity.y = 0;
            } else {
                this.velocity.y += this.velocityDelta;
            }
        } else if (this.velocity.y < 0) {
            if (this.velocity.y + this.velocityDelta > 0) {
                this.velocity.y = 0;
            } else {
                this.velocity.y -= this.velocityDelta;
            }
        }

        // Game border collision.
        var bounds = loadedGame.getGamemode().getGameBounds();

        // Leveling up.
        if (this.score / Constants.LEVEL_THRESHOLD > this.level) {
            this.levelUp();
            world.onLevelUp(this, this.level);
        }

        // Shooting cooldown.
        this.shootCooldown = Math.max(this.shootCooldown - 1, 0);

        // Invincibility ticks after spawn.
        if (this.joinInvincibility && this.invincibilityTicks-- < 0) {
            this.invincible = false;
            this.joinInvincibility = false;
        }

        for (var appliedEffect : this.statusEffects) {
            appliedEffect.tick(this);
        }

        this.statusEffects.removeIf(effect -> effect.getRemainingTime().isNegative());

        this.accel.x *= (this.brake ? DRAG / 1.1f : DRAG) / TPS * 40;
        this.accel.y *= (this.brake ? DRAG / 1.1f : DRAG) / TPS * 40;

        this.prevPos.set(this.pos);

        this.pos.add((this.accel.x + this.velocity.x) / TPS, (this.accel.y + this.velocity.y) / TPS);

        double minX = bounds.x + this.radius();
        double minY = bounds.y + this.radius();
        double maxX = bounds.x + bounds.width - this.radius();
        double maxY = bounds.y + bounds.height - this.radius();

        if (!(world.getGamemode() instanceof OpenWorldMode)) {
            if (this.pos.x > maxX && this.velocity.x > 0) this.velocity.x = 0;
            if (this.pos.x < minX && this.velocity.x < 0) this.velocity.x = 0;
            if (this.pos.x > maxX && this.accel.x > 0) this.accel.x = 0;
            if (this.pos.x < minX && this.accel.x < 0) this.accel.x = 0;

            if (this.pos.y > maxY && this.velocity.y > 0) this.velocity.y = 0;
            if (this.pos.y < minY && this.velocity.y < 0) this.velocity.y = 0;
            if (this.pos.y > maxY && this.accel.y > 0) this.accel.y = 0;
            if (this.pos.y < minY && this.accel.y < 0) this.accel.y = 0;

            this.pos.x = (float) Mth.clamp(this.pos.x, minX, maxX);
            this.pos.y = (float) Mth.clamp(this.pos.y, minY, maxY);
        }

        var pixelsPerTick = this.prevPos.dst(this.pos);
        this.currentSpeed = pixelsPerTick * TPS;
    }

    @Override
    public void damage(double value, EntityDamageSource source) {
        if (this.invincible) return;

        var defense = this.attributes.getBase(Attribute.DEFENSE);
        if (defense <= 0.0d) {
            this.destroy();
            return;
        }

        // Deal damage to the player.
        this.health -= value / defense;

        // Check health.
        this.checkHealth();

        // Check if source has attack modifier.
        if (value > 0.0d) {
            // Check if window is not focused.
            if (!BubbleBlaster.getInstance().getGameWindow().isFocused()) {
                if (SystemUtils.IS_JAVA_9) {
                    // Let the taskbar icon flash. (Java 9+)
                    BubbleBlaster.getInstance().getGameWindow().requestUserAttention();
                }
            }
        }
    }

    @Override
    public void onCollision(Entity other, double deltaTime) {
        if (other.isBad()) {
            this.successRate -= 3;
            return;
        }

        // Modifiers
        if (other instanceof Bubble && !((Bubble) other).isInvincible()) {
            var bubble = (Bubble) other;
            var bubbleAttrs = bubble.getAttributes();
            var bubbleScore = bubbleAttrs.get(Attribute.SCORE);
            var bubbleDefense = bubbleAttrs.get(Attribute.DEFENSE);
            var scoreModifier = this.attributes.get(Attribute.SCORE_MODIFIER);

            // Attributes
            var props = bubble.getRadius() * (bubble.getSpeed() + 1);
            var attrs = props * bubbleDefense * bubbleScore * bubbleScore * scoreModifier;

            // Calculate score value.
            var award = attrs * deltaTime / Constants.BUBBLE_SCORE_REDUCTION_SELF;

            // Add score.
            this.awardScore(award);
        } else if (other.getAttributes().has(Attribute.SCORE_MODIFIER)) {
            var score = other.getAttributes().get(Attribute.SCORE_MODIFIER);
            this.awardScore(score);
        }
    }

    /**
     * Renders the player.
     *
     * @param renderer the renderer to use.
     */
    @Override
    public void render(Renderer renderer) {
        if (this.isNotSpawned()) return;

        if (this.invincible)
            renderer.withEffect(1.5f, () -> renderer.fillCircle((float) this.pos.x, (float) this.pos.y, RADIUS * 2, Colors.WHITE));
        else
            renderer.fillCircle((float) this.pos.x, (float) this.pos.y, RADIUS * 2, Colors.CRIMSON);

        renderer.fillPolygon(this.getArrowShape(), Colors.WHITE);
    }

    /**
     * Rotate the player.
     *
     * @param deltaRotation amount of degrees to rotate.
     * @deprecated use {@link #rotate(float)} instead.
     */
    @Deprecated
    public void rotateDelta(int deltaRotation) {
        this.rotation += deltaRotation;
    }

    /**
     * Handles deleting of the player.
     *
     * @see #delete()
     */
    @Override
    public void onDelete() {
        this.invalidate();
    }

    /**
     * Checks health, if health is zero or less, it will trigger game-over.
     *
     * @see #getHealth()
     */
    @Override
    public void checkHealth() {
        if (this.health <= 0 && this.getWorld().isAlive()) {
            this.die();
        }
    }

    /**
     * Kills the player in an instant.
     */
    public void die() {
        if (this.getWorld().isAlive()) {
            this.getWorld().triggerGameOver();
        }
    }

    /**
     * Save the player data.
     *
     * @return the player data.
     */
    @Override
    public @NotNull MapType save() {
        @NotNull MapType document = super.save();

        document.putDouble("score", this.score);
        document.putFloat("rotation", this.rotation);
        return document;
    }

    /**
     * Load the player data.
     *
     * @param data the player data.
     */
    @Override
    public void load(MapType data) {
        super.save();

        this.score = data.getFloat("score");
        this.rotation = data.getFloat("rotation");
    }

    /**
     * Get the speed of the rotation.
     * The speed is in degrees per second.
     *
     * @return the rotation speed.
     */
    public float getRotationSpeed() {
        return this.rotationSpeed;
    }

    /**
     * Set the speed of the rotation.
     * The speed is in degrees per second.
     *
     * @param rotationSpeed the rotation speed to set.
     */
    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * Rotate the player.<br>
     * NOTE: Rotation is in degrees.
     *
     * @param rotation amount of degrees to rotate.
     */
    public void rotate(float rotation) {
        this.rotation = (this.rotation + rotation) % 360;
    }

    /**
     * Get the player's current score.
     *
     * @return the score of the player.
     */
    public double getScore() {
        return this.score;
    }

    /**
     * Increment score of the player.
     *
     * @param value amount of score to increment.
     */
    public void awardScore(double value) {
        this.score += value;
        this.successRate++;
    }

    /**
     * Decrement score of the player.
     *
     * @param value amount of score to decrement.
     */
    public void subtractScore(double value) {
        this.score -= value;
        this.successRate--;
    }

    /**
     * Set the player's score.
     *
     * @param value the score of the player to set.
     */
    public void setScore(double value) {
        if (this.score > value) this.successRate--;
        if (this.score < value) this.successRate++;
        this.score = value;
    }

    /**
     * Get the player's current level.
     *
     * @return the level.
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * Level-up the player.
     */
    public void levelUp() {
        PlayerEvents.LEVEL_UP.factory().onLevelUp(this, this.level + 1);
        this.level++;
        this.successRate += 5;

        BubbleBlaster.newActivity(GameActivity.PLAYING);
    }

    /**
     * Decrement the player's level.
     */
    public void levelDown() {
        PlayerEvents.LEVEL_DOWN.factory().onLevelDown(this, this.level - 1);
        this.setLevel(this.getLevel() - 1);
        this.successRate -= 5;
    }

    /**
     * Set the player's level.
     *
     * @param level the level to set.
     */
    public void setLevel(int level) {
        if (level < this.level) PlayerEvents.LEVEL_DOWN.factory().onLevelDown(this, level);
        else if (level > this.level) PlayerEvents.LEVEL_UP.factory().onLevelUp(this, level);
        else return;

        this.successRate += (level - this.level) * 5;
        this.level = Math.max(level, 1);
    }

    /**
     * Get the ability container create the entity.
     *
     * @return the requested {@link AbilityContainer}.
     * @see AbilityContainer
     */
    public AbilityContainer getAbilityContainer() {
        return this.abilityContainer;
    }

    /**
     * Get current ammo type.
     *
     * @return the current {@link AmmoType ammo type}.
     * @see AmmoType
     * @see #setCurrentAmmo(AmmoType)
     */
    public AmmoType getCurrentAmmo() {
        return this.currentAmmo;
    }

    /**
     * Set current ammo type.
     *
     * @param currentAmmo the ammo to set.
     * @see AmmoType
     * @see #getCurrentAmmo()
     */
    public void setCurrentAmmo(AmmoType currentAmmo) {
        this.currentAmmo = currentAmmo;
    }

    /**
     * Deactivate / activate the forwards motion.
     *
     * @param val amount to move forward/backward,
     */
    @Override
    public void moving(float val) {
        this.moving = val;
    }

    /**
     * Deactivate / activate the left rotation.
     *
     * @param val the amount to rotate left/right.
     */
    @Override
    public void rotating(float val) {
        this.rotating = val;
    }

    public float getVelocityDelta() {
        return this.velocityDelta;
    }

    public void setVelocityDelta(float velocityDelta) {
        this.velocityDelta = velocityDelta;
    }

    public long getAbilityEnergy() {
        return this.abilityEnergy;
    }

    public void setAbilityEnergy(long abilityEnergy) {
        this.abilityEnergy = abilityEnergy;
    }

    @Override
    public boolean hasAi() {
        return false;
    }

    @Override
    public float radius() {
        return 20;
    }

    /**
     * Shoot a bullet from the {@linkplain #getCurrentAmmo() current ammo}.
     *
     * @see #setCurrentAmmo(AmmoType)
     */
    public void shoot() {
        this.shoot(false);
    }

    /**
     * Shoot a bullet from the {@linkplain #getCurrentAmmo() current ammo}.
     *
     * @see #setCurrentAmmo(AmmoType)
     */
    public void shoot(boolean force) {
        if (force || this.canShoot()) {
            this.shootCooldown = TimeProcessor.millisToTicks(Constants.SHOOT_COOLDOWN);

            var bulletPos = this.pos.cpy();
            var bullet = new Bullet(this.currentAmmo, bulletPos, this.rotation, this.world);
            bullet.setOwner(this);
            this.world.spawn(bullet, SpawnInformation.triggeredSpawn(this, this.world));
        }
    }

    public void boost() {
        this.boost(false);
    }

    public void boost(boolean force) {
        if (force || this.canBoost()) {
            this.boostAccelTimer = TimeUtils.toTicks(Duration.ofMilliseconds(Constants.BOOST_DURATION));
            if (!force) this.boostRefillTimer = -1;
        }
    }

    private boolean canBoost() {
        return this.boostRefillTimer <= 0;
    }

    public boolean canShoot() {
        return this.shootCooldown <= 0;
    }

    public int getInvincibilityTicks() {
        return this.invincibilityTicks;
    }

    @Override
    public String getName() {
        return "Player";
    }

    public int getSuccessRate() {
        return this.successRate;
    }

    public void setBrake(boolean brake) {
        this.brake = brake;
    }

    public boolean isBrake() {
        return this.brake;
    }

    /**
     * @return the current speed in pixels per second. (px/s)
     */
    public double getCurrentSpeed() {
        return this.currentSpeed;
    }

    public double visualDistanceToMouseCursor() {
        var mousePos = dev.ultreon.bubbles.input.DesktopInput.getMousePos();
        var mousePosition = new Vector2D(mousePos.x, mousePos.y);
        var visualPos = new Vector2D(this.getVisualX(), this.getVisualY());
        return mousePosition.dst(visualPos);
    }

    public boolean isBlind() {
        return this.hasEffect(StatusEffects.BLINDNESS);
    }

    private boolean hasEffect(BlindnessStatusEffect effect) {
        for (var statusEffect : this.statusEffects) {
            if (statusEffect.getType() == effect) {
                return true;
            }
        }
        return false;
    }
}
