package com.sima.buriedage.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/**
 * Every number the Echo of the Past feature has, in one place. Nothing here affects anything
 * outside the echo, so all of it is safe to retune without touching other systems.
 *
 * <p>Times are in ticks: 20 ticks = 1 second. Speeds are in blocks per tick.
 */
public final class EchoTuning {

    // ================================================================ master switch

    /**
     * Set to false to turn the whole feature off: no automatic spawning at all. The spawn egg still
     * works, so it can be tested while disabled. This is the one-line kill switch.
     */
    public static final boolean AUTO_SPAWN_ENABLED = true;

    // ================================================================ spawning

    /** Ticks after a spawn before that player may roll again. 75-120 seconds. */
    public static final int COOLDOWN_MIN = 75 * 20;
    public static final int COOLDOWN_MAX = 120 * 20;

    /** Once the cooldown is over, a roll happens this often. */
    public static final int ROLL_INTERVAL = 15 * 20;

    /** Chance per roll that anything spawns at all. Keeps encounters unpredictable. */
    public static final float SPAWN_CHANCE = 0.40F;

    /** Chance that an ordinary spawn places two unrelated echoes instead of one. */
    public static final float DOUBLE_SPAWN_CHANCE = 0.25F;

    /** How many echoes may exist in the world at once, across all players. */
    public static final int MAX_ECHOES = 4;

    /** Ring around the player where an echo may appear. */
    public static final double SPAWN_RADIUS_MIN = 8.0;
    public static final double SPAWN_RADIUS_MAX = 15.0;

    /** How far above and below the player the spawner looks for open space in the city. */
    public static final int SPAWN_VERTICAL_SEARCH = 4;

    // ================================================================ mode weights

    /** Must add up to 100. Drift is the cheap ambient one, meeting is the rare set piece. */
    public static final int WEIGHT_DRIFT = 45;
    public static final int WEIGHT_WANDER = 35;
    public static final int WEIGHT_MEETING = 20;

    // ================================================================ shared look

    /** Ticks the echo takes to fade in on arrival and to fade out before vanishing. */
    public static final int FADE_IN_TICKS = 10;
    public static final int FADE_TICKS = 40;

    /** Vertical sway. Speed is radians per tick, amplitudes are blocks. */
    public static final double BOB_SPEED = 0.12;

    /** Particles puffed out when an echo is hit and dissolves. */
    public static final int DISSOLVE_PARTICLES = 12;

    public static final float WHISPER_VOLUME = 0.35F;
    public static final float WHISPER_PITCH = 0.6F;

    // ================================================================ mode: drift

    /** Slowly floats in a straight line through everything. */
    public static final int DRIFT_LIFETIME_MIN = 6 * 20;
    public static final int DRIFT_LIFETIME_MAX = 8 * 20;
    public static final float DRIFT_SPEED = 0.022F;
    public static final float DRIFT_BOB = 0.004F;

    // ================================================================ mode: wander

    /** Walks a few blocks, stops as if busy, turns, walks again. */
    public static final int WANDER_LIFETIME_MIN = 12 * 20;
    public static final int WANDER_LIFETIME_MAX = 18 * 20;
    public static final float WANDER_SPEED = 0.035F;
    public static final float WANDER_BOB = 0.006F;
    public static final float WANDER_PAUSE_BOB = 0.010F;

    /** How many walk-then-pause beats the little story has. */
    public static final int WANDER_STEPS_MIN = 2;
    public static final int WANDER_STEPS_MAX = 4;

    /** Length of one walking leg, in blocks. */
    public static final int WANDER_LEG_BLOCKS_MIN = 3;
    public static final int WANDER_LEG_BLOCKS_MAX = 6;

    /** How long it stands still between legs. */
    public static final int WANDER_PAUSE_MIN = 2 * 20;
    public static final int WANDER_PAUSE_MAX = 3 * 20;

    /** While paused it glances around: how often, and how wide, in degrees. */
    public static final int WANDER_GLANCE_INTERVAL = 12;
    public static final float WANDER_GLANCE_SPREAD = 90.0F;

    /** How sharply it may turn between legs, in degrees. */
    public static final float WANDER_TURN_SPREAD = 220.0F;

    // ================================================================ mode: meeting

    /** Two echoes walk together, talk, then part. */
    public static final int MEETING_LIFETIME_MIN = 15 * 20;
    public static final int MEETING_LIFETIME_MAX = 20 * 20;
    public static final float MEETING_SPEED = 0.030F;
    public static final float MEETING_BOB = 0.006F;
    public static final float MEETING_TALK_BOB = 0.012F;

    /** Distance between the two spawn points. */
    public static final double MEETING_SEPARATION_MIN = 6.0;
    public static final double MEETING_SEPARATION_MAX = 10.0;

    /** How close to the midpoint counts as "arrived", squared. */
    public static final double MEETING_ARRIVAL_RADIUS_SQR = 1.2 * 1.2;

    /** How long they stand facing each other. */
    public static final int MEETING_TALK_MIN = 4 * 20;
    public static final int MEETING_TALK_MAX = 6 * 20;

    // ================================================================ look

    /**
     * Which villager professions an echo can wear. The base body is always the plain villager
     * texture; these are the clothing overlays drawn on top of it.
     */
    public static final Identifier[] PROFESSIONS = {
            profession("farmer"),
            profession("armorer"),
            profession("shepherd"),
            profession("mason"),
            profession("librarian"),
            profession("nitwit"),
    };

    /** Base villager body under the clothes. */
    public static final Identifier BODY_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    /**
     * Ghost tint, as ARGB. Alpha is the transparency, the RGB is multiplied into the texture:
     * a desaturated cold blue. Raise the alpha byte to make echoes more solid.
     */
    public static final int TINT_ARGB = 0x8FA8C4D8;

    private static Identifier profession(String name) {
        return Identifier.withDefaultNamespace("textures/entity/villager/profession/" + name + ".png");
    }

    /** Inclusive on both ends. */
    public static int randomBetween(RandomSource random, int min, int max) {
        return max <= min ? min : min + random.nextInt(max - min + 1);
    }

    public static double randomBetween(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private EchoTuning() {}
}
