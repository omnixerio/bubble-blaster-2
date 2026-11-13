package dev.ultreon.bubbles.entity;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Shape2D;
import dev.ultreon.bubbles.Constants;
import dev.ultreon.bubbles.ammo.AmmoType;
import dev.ultreon.bubbles.entity.attribute.Attribute;
import dev.ultreon.bubbles.entity.player.Player;
import dev.ultreon.bubbles.gamemode.openworld.OpenWorldMode;
import dev.ultreon.bubbles.init.AmmoTypes;
import dev.ultreon.bubbles.init.Entities;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.vector.Vector2D;
import dev.ultreon.bubbles.world.World;
import dev.ultreon.bubbles.util.annotation.FieldsAreNonnullByDefault;
import dev.ultreon.bubbles.util.annotation.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public class Bullet extends Entity {
    @Nullable
    private Player owner = null;
    private AmmoType ammoType;
    private int popsRemaining = 4;

    public Bullet(World world) {
        this(AmmoTypes.BASIC, new Vector2D(), 0, world);
    }

    public Bullet(World world, AmmoType ammoType) {
        this(ammoType, new Vector2D(), 0, world);
    }

    public Bullet(AmmoType type, Vector2D pos, float rotation, World world) {
        super(Entities.BULLET, world);

        this.pos.set(pos);
        this.pos.set(pos);
        this.setRotation(rotation);

        this.ammoType = type;
        this.attributes.setAll(type.getDefaultAttributes());

        this.setSpeed(type.getSpeed());

        this.markAsCollidable(Entities.BUBBLE);
        this.markAsAttackable(Entities.BUBBLE);

        this.popsRemaining = world.getGamemode().getBulletPops();
    }

    public int getPopsRemaining() {
        return this.popsRemaining;
    }

    public void setPopsRemaining(int popsRemaining) {
        this.popsRemaining = popsRemaining;
    }

    public @Nullable Player getOwner() {
        return this.owner;
    }

    public void setOwner(@Nullable Player owner) {
        this.owner = owner;
    }

    private void setSpeed(float speed) {
        this.attributes.setBase(Attribute.SPEED, speed);
    }

    @Override
    public void onCollision(Entity other, double deltaTime) {
        if (this.owner == null) return;
        if (other instanceof LivingEntity && ((LivingEntity) other).isInvincible()) {
            var livingEntity = (LivingEntity) other;
            return;
        }

        this.ammoType.onCollision(this, other, deltaTime);


        // Modifiers
        if (other instanceof AbstractBubbleEntity) {
            var bubble = (AbstractBubbleEntity) other;
            // Attributes
            var attributeMap = bubble.getAttributes();
            var bubScore = attributeMap.getBase(Attribute.SCORE);
            var attack = attributeMap.getBase(Attribute.ATTACK);  // Maybe used.
            var defense = attributeMap.getBase(Attribute.DEFENSE);  // Maybe used.
            var scoreModifier = this.owner.getAttributes().getBase(Attribute.SCORE_MODIFIER);

            // Properties
            var radius = bubble.getRadius();
            var speed = bubble.getSpeed();

            // Calculate score value.
            var props = (radius * (speed + 1)) * (attack + defense + 1);
            var attrs = props * bubScore * bubScore * scoreModifier;
            var scoreValue = attrs * deltaTime / Constants.BUBBLE_SCORE_REDUCTION;

            // Add score.
            this.owner.awardScore(scoreValue);
        } else if (other.getAttributes().has(Attribute.SCORE_MODIFIER)) {
            var score = other.getAttributes().get(Attribute.SCORE_MODIFIER);
            this.owner.awardScore(score);
        }
        if (--this.popsRemaining <= 0) {
            this.delete();
        }
    }

    @Override
    public Shape2D getShape() {
        return this.ammoType.getShape(this);
    }

    @Override
    public void render(Renderer renderer) {
        this.ammoType.render(renderer, this);
    }

    @Override
    public Rectangle getBounds() {
        if (this.world.getGamemode() instanceof OpenWorldMode) {
            var rel = this.game.player.pos;
            return new Rectangle((float) (this.pos.x - rel.x), (float) (this.pos.y - rel.y), 1, 1);
        }
        return new Rectangle((float) this.pos.x, (float) this.pos.y, 1, 1);
    }

    @Override
    public float radius() {
        return 3;
    }

    @Override
    protected boolean isValid() {
        return false;
    }

    @Override
    public void tick(World world) {
        super.tick(world);

        if (!this.isVisible()) {
            this.delete();
        }
    }

    @Nullable
    public AmmoType getAmmoType() {
        return this.ammoType;
    }

    public void setAmmoType(AmmoType ammoType) {
        this.ammoType = ammoType;
        this.attributes.setAll(ammoType.getDefaultAttributes());
    }
}
