package com.sima.buriedage.entity;

/**
 * Every number the pegasus has, in one place. Speeds are blocks per tick (20 ticks = 1 second),
 * angles are degrees, times are ticks unless the name says seconds.
 *
 * <p>Pitch follows Minecraft's convention: negative is nose up, positive is nose down.
 */
public final class PegasusTuning {
    private PegasusTuning() {}

    // ================================================================ switches

    /** Set to false and the pegasus behaves like a plain horse: no takeoff, no flight, no stamina. */
    public static final boolean FLIGHT_ENABLED = true;

    /** Set to false and a placed egg never hatches (it still exists as a block and a find). */
    public static final boolean HATCHING_ENABLED = true;

    // ================================================================ birth stats

    /** Max health at birth: HEALTH_BASE + random(0..A-1) + random(0..B-1), so 30..49. */
    public static final float HEALTH_BASE = 30.0F;
    public static final int HEALTH_RANDOM_A = 10;
    public static final int HEALTH_RANDOM_B = 11;

    /** Ground speed attribute: SPEED_BASE + two random draws of up to SPEED_RANDOM. Vanilla horses average 0.225. */
    public static final double SPEED_BASE = 0.225;
    public static final double SPEED_RANDOM = 0.05;

    /** Bounds used when two parents breed; the foal lands between them with the same spread vanilla uses. */
    public static final float HEALTH_MIN = HEALTH_BASE;
    public static final float HEALTH_MAX = HEALTH_BASE + HEALTH_RANDOM_A - 1 + HEALTH_RANDOM_B - 1;
    public static final double SPEED_MIN = SPEED_BASE;
    public static final double SPEED_MAX = SPEED_BASE + 2 * SPEED_RANDOM;

    // ================================================================ hatching and growth

    /** How long a placed egg takes to hatch, plus a random offset so two eggs never hatch together. */
    public static final int HATCH_TICKS = 15 * 60 * 20;
    public static final int HATCH_RANDOM_TICKS = 30 * 20;

    /** A golden apple pushes a foal this many seconds closer to adulthood (vanilla foals need 1200). */
    public static final int GOLDEN_APPLE_AGE_SECONDS = 300;
    public static final float GOLDEN_APPLE_HEAL = 10.0F;

    // ================================================================ stamina (0..1, shown on the jump bar)

    /** A full bar lasts this long in level powered flight. */
    public static final float CRUISE_SECONDS = 30.0F;
    /** A full bar lasts this long while climbing with the jump key held. */
    public static final float CLIMB_SECONDS = 10.0F;
    /** From empty to full while standing on the ground. */
    public static final float REGEN_SECONDS = 60.0F;

    public static final float CRUISE_COST = 1.0F / (CRUISE_SECONDS * 20.0F);
    public static final float CLIMB_COST = 1.0F / (CLIMB_SECONDS * 20.0F);
    public static final float GROUND_REGEN = 1.0F / (REGEN_SECONDS * 20.0F);

    /** Below this the pegasus refuses to take off. */
    public static final float TAKEOFF_MIN_STAMINA = 0.1F;

    // ================================================================ takeoff

    /** Ticks the jump key must be held on the ground for a standing takeoff. */
    public static final int TAKEOFF_CHARGE_TICKS = 8;
    /** Ground speed at which a short tap of jump is enough: a running takeoff. */
    public static final double RUN_TAKEOFF_SPEED = 0.3;
    /** Airspeed and nose-up angle the pegasus leaves the ground with. */
    public static final float TAKEOFF_SPEED = 0.4F;
    public static final float TAKEOFF_PITCH = -30.0F;
    /** Landing checks are ignored for this long after takeoff, so the first tick does not count as touching down. */
    public static final int TAKEOFF_GRACE_TICKS = 6;

    // ================================================================ attitude

    /** The camera pitch is clamped to this before it becomes the flight path: no vertical dives or climbs. */
    public static final float MAX_PITCH = 60.0F;
    /** Nose-up angle the jump key asks for, at least. Sustainable at cruise speed. */
    public static final float CLIMB_PITCH = -25.0F;
    /** Nose-down angle with no input at all: a slow glide. */
    public static final float GLIDE_PITCH = 8.0F;
    /** With an empty bar the pegasus may not hold altitude: the nose stays at least this far down. */
    public static final float FORCED_GLIDE_MIN_PITCH = 5.0F;
    /** How fast the nose follows the camera, degrees per tick. Sluggish while stalled. */
    public static final float PITCH_RATE = 4.0F;
    public static final float STALLED_PITCH_RATE = 1.2F;

    /** Turn rate at walking pace, degrees per tick; divided by (1 + airspeed / TURN_SPEED_SCALE) as speed grows. */
    public static final float TURN_RATE = 6.0F;
    public static final float TURN_SPEED_SCALE = 0.6F;
    public static final float STALLED_TURN_FACTOR = 0.3F;

    // ================================================================ energy

    /**
     * Potential-kinetic exchange: airspeed changes by this much per tick at a 90 degree pitch, scaled
     * by sin(pitch). Diving speeds up, climbing bleeds speed. This is the number that makes flight feel heavy.
     */
    public static final float GRAVITY_EXCHANGE = 0.08F;
    /** Wing thrust with W held, tapering to nothing at CRUISE_MAX_SPEED. */
    public static final float THRUST = 0.03F;
    /** Extra thrust while the jump key is held and the bar is not empty. */
    public static final float CLIMB_THRUST_BONUS = 0.02F;
    /** Powered flight cannot push past this on its own; only a dive can. */
    public static final float CRUISE_MAX_SPEED = 0.9F;
    /** Drag: airspeed loses DRAG * airspeed^2 per tick. Sets the cruise speed together with THRUST. */
    public static final float DRAG = 0.0136F;
    /** Hard cap on airspeed, whatever the dive. */
    public static final float DIVE_MAX_SPEED = 1.5F;
    /** Nothing below this counts as flying: at lower speeds the pegasus is falling. */
    public static final float MIN_AIRSPEED = 0.12F;

    // ================================================================ stall

    /** Nose up by more than this while slower than STALL_SPEED: the wing stops carrying. */
    public static final float STALL_PITCH = -5.0F;
    public static final float STALL_SPEED = 0.25F;
    /** The stall ends when the speed is back above this and the timer has run out. */
    public static final float STALL_EXIT_SPEED = 0.4F;
    public static final int STALL_TICKS = 25;
    /** Where the nose drops to on its own during the stall. */
    public static final float STALL_NOSE_DOWN = 30.0F;

    // ================================================================ landing and crashes

    /** Touching down slower than this is a soft landing; faster is a stumble. */
    public static final float SOFT_LANDING_SPEED = 0.45F;
    /** Rider damage per block/tick above the soft limit. A landing at 1.0 costs (1.0 - 0.45) * 6 = 3.3 HP. */
    public static final float HARD_LANDING_DAMAGE = 6.0F;
    /** After a stumble the rider has no control for this long. */
    public static final int STUMBLE_TICKS = 20;
    /** Flying into a wall faster than this hurts rider and mount. */
    public static final float CRASH_SPEED = 0.5F;
    public static final float CRASH_DAMAGE = 8.0F;
    /** Airspeed kept after hitting a wall. */
    public static final float CRASH_SPEED_KEPT = 0.2F;

    // ================================================================ sound

    /** Ticks between wing flaps by mode; 0 means silent. */
    public static final int FLAP_INTERVAL_CLIMB = 7;
    public static final int FLAP_INTERVAL_CRUISE = 14;
    public static final int FLAP_INTERVAL_GLIDE = 0;
    public static final int FLAP_INTERVAL_TAKEOFF = 5;
    public static final float FLAP_VOLUME = 0.7F;

    // ================================================================ look

    /** Bank angle per degree-per-tick of yaw rate, and the most the model ever banks. */
    public static final float ROLL_PER_YAW_RATE = 4.0F;
    public static final float MAX_ROLL = 35.0F;
    /** Fraction of the gap the visible roll closes each tick. */
    public static final float ROLL_SMOOTHING = 0.15F;
    /** Fraction of the flight-path pitch the model itself tilts by. */
    public static final float BODY_PITCH_FACTOR = 0.6F;
    /** The takeoff and landing animations play for this long. */
    public static final int TAKEOFF_ANIMATION_TICKS = 12;
    public static final int LANDING_ANIMATION_TICKS = 12;

    /** Field of view grows by airspeed times this, like the elytra. 1.0 airspeed adds 15%. */
    public static final float FOV_PER_SPEED = 0.15F;

    // ================================================================ death and achievements

    /** Slow falling given to the rider when the mount dies in the air. */
    public static final int DEATH_SLOW_FALLING_TICKS = 8 * 20;

    /** Horizontal blocks in one uninterrupted flight for the Icarus advancement. */
    public static final double ICARUS_DISTANCE = 1000.0;
}
