package dev.ultreon.bubbles.init;

import dev.ultreon.bubbles.bubble.*;
import dev.ultreon.bubbles.effect.StatusEffectInstance;
import dev.ultreon.bubbles.registry.Registries;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.libs.datetime.v0.Duration;
import org.jetbrains.annotations.ApiStatus;

/**
 * Bubble Initialization
 * Bubble init, used for initialize bubbles.
 * For example, the {@link ResistanceBoostBubble} instance is assigned here.
 *
 * @see BubbleType
 */
public class BubbleTypes {
    // Bubbles
    public static final BubbleType NORMAL = BubbleTypes.register("normal", BubbleType.builder()
            .priority(150_000_000L)
            .radius(12, 105)
            .speed(4.0, 8.7)
            .colors(Colors.WHITE)
            .score(1)
            .build());
    public static final BubbleType DOUBLE = BubbleTypes.register("double", BubbleType.builder()
            .priority(4_600_000L)
            .radius(24, 75)
            .speed(8.0, 17.4)
            .colors(Colors.GOLD, Colors.GOLD)
            .score(2)
            .build());
    public static final BubbleType TRIPLE = BubbleTypes.register("triple", BubbleType.builder()
            .priority(1_150_000L)
            .radius(48, 60)
            .speed(12.0, 38.8)
            .colors(Colors.CYAN, Colors.CYAN, Colors.CYAN)
            .score(3)
            .build());
    public static final BubbleType DOUBLE_STATE = BubbleTypes.register("double_state", BubbleType.builder()
            .colors("#ffc000,#ffc000,#00000000,#ffc000")
            .priority(460000L)
            .radius(21, 55)
            .speed(4.0, 10.8)
            .score(2)
            .effect((source, target) -> new StatusEffectInstance(StatusEffects.SCORE, Duration.ofSeconds((long) (source.getRadius() / 6)), 2))
            .build());
    public static final BubbleType TRIPLE_STATE = BubbleTypes.register("triple_state", BubbleType.builder()
            .colors("#00ffff,#00ffff,#00000000,#00ffff,#00000000,#00ffff")
            .priority(115000L)
            .radius(21, 55)
            .speed(4.1, 10.4)
            .defense(0.3f, 0.4f)
            .score(3)
            .effect((source, target) -> new StatusEffectInstance(StatusEffects.SCORE, Duration.ofSeconds((long) (source.getRadius() / 8)), 3))
            .hardness(1)
            .build()
    );
    public static final BubbleType BOUNCY = BubbleTypes.register("bouncy", BubbleType.builder()
            .priority(715_000L)
            .invulnerable()
            .radius(15, 85)
            .speed(3.215d, 4.845d)
            .score(0.2, 0.6)
            .colors("#ffffffff,#ffffff80,#ffffff40,#ffffff20,#ffffff10,#ffffff08,#ffffff04,#ffffff02")
            .bounceAmount(30)
            .build());
    public static final BubbleType BUBBLE_FREEZE = BubbleTypes.register("bubble_freeze", new BubbleFreezeBubble());
    public static final BubbleType PARALYZE = BubbleTypes.register("paralyze", BubbleType.builder()
            .priority(3_325_000L)
            .radius(28, 87)
            .speed(1.5, 3)
            .score(0.2, 0.4)
            .effect((source, target) -> new StatusEffectInstance(StatusEffects.PARALYZE, Duration.ofSeconds((long) (source.getRadius() / 8)), (byte) 1))
            .colors("#ffff00,#ffff5f,#ffffdf,#ffffff")
            .difficulty(10)
            .build());
    public static final DamageBubble DAMAGE = BubbleTypes.register("damage", new DamageBubble());
    public static final BubbleType POISON = BubbleTypes.register("poison", new PoisonBubble());
    public static final HealBubble HEAL = BubbleTypes.register("heal", new HealBubble());
    public static final UltraBubble ULTRA = BubbleTypes.register("ultra", new UltraBubble());
    public static final LevelUpBubble LEVEL_UP = BubbleTypes.register("level_up", new LevelUpBubble());
    public static final HardenedBubble HARDENED = BubbleTypes.register("hardened", new HardenedBubble());
    public static final BlindnessBubble BLINDNESS = BubbleTypes.register("blindness", new BlindnessBubble());
    public static final AccelerateBubble ACCELERATE = BubbleTypes.register("accelerate", new AccelerateBubble());
    public static final SpeedBoostBubble SPEED_BOOST = BubbleTypes.register("speed_boost", new SpeedBoostBubble());
    public static final AttackBoostBubble ATTACK_BOOST = BubbleTypes.register("attack", new AttackBoostBubble());
    public static final ResistanceBoostBubble RESISTANCE_BOOST = BubbleTypes.register("resistance", new ResistanceBoostBubble());

    private static <T extends BubbleType> T register(String name, T bubbleType) {
        Registries.BUBBLES.register(new Identifier(name), bubbleType);
        return bubbleType;
    }

    @ApiStatus.Internal
    public static void register() {

    }
}
