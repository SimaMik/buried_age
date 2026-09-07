package com.sima.buriedage.entity;

public final class PegasusTuning {
    private PegasusTuning() {}

    public static final boolean FLIGHT_ENABLED = true;

    public static final boolean HATCHING_ENABLED = true;

    public static final float HEALTH_BASE = 30.0F;
    public static final int HEALTH_RANDOM_A = 10;
    public static final int HEALTH_RANDOM_B = 11;

    public static final double SPEED_BASE = 0.225;
    public static final double SPEED_RANDOM = 0.05;

    public static final float HEALTH_MIN = HEALTH_BASE;
    public static final float HEALTH_MAX = HEALTH_BASE + HEALTH_RANDOM_A - 1 + HEALTH_RANDOM_B - 1;
    public static final double SPEED_MIN = SPEED_BASE;
    public static final double SPEED_MAX = SPEED_BASE + 2 * SPEED_RANDOM;

    public static final int HATCH_TICKS = 15 * 60 * 20;
    public static final int HATCH_RANDOM_TICKS = 30 * 20;

    public static final int GOLDEN_APPLE_AGE_SECONDS = 300;
    public static final float GOLDEN_APPLE_HEAL = 10.0F;

    public static final float CRUISE_SECONDS = 120.0F;

    public static final float CLIMB_SECONDS = 40.0F;

    public static final float REGEN_SECONDS = 15.0F;

    public static final float GLIDE_REGEN_SECONDS = 45.0F;

    public static final float CRUISE_COST = 1.0F / (CRUISE_SECONDS * 20.0F);
    public static final float CLIMB_COST = 1.0F / (CLIMB_SECONDS * 20.0F);
    public static final float GROUND_REGEN = 1.0F / (REGEN_SECONDS * 20.0F);
    public static final float GLIDE_REGEN = 1.0F / (GLIDE_REGEN_SECONDS * 20.0F);

    public static final float TAKEOFF_MIN_STAMINA = 0.1F;

    public static final float SADDLE_FEED_STAMINA = 0.1F;

    public static final int TAKEOFF_CHARGE_TICKS = 8;

    public static final double RUN_TAKEOFF_SPEED = 0.3;

    public static final float TAKEOFF_SPEED = 0.4F;
    public static final float TAKEOFF_PITCH = -30.0F;

    public static final int TAKEOFF_GRACE_TICKS = 6;

    public static final float MAX_PITCH = 60.0F;

    public static final float CLIMB_PITCH = -25.0F;

    public static final float GLIDE_PITCH = 8.0F;

    public static final float FORCED_GLIDE_MIN_PITCH = 5.0F;

    public static final float PITCH_RATE = 4.0F;
    public static final float STALLED_PITCH_RATE = 1.2F;

    public static final float TURN_RATE = 6.0F;
    public static final float TURN_SPEED_SCALE = 0.6F;
    public static final float STALLED_TURN_FACTOR = 0.3F;

    public static final float GRAVITY_EXCHANGE = 0.08F;

    public static final float THRUST = 0.03F;

    public static final float CLIMB_THRUST_BONUS = 0.02F;

    public static final float CRUISE_MAX_SPEED = 0.9F;

    public static final float DRAG = 0.0136F;

    public static final float DIVE_MAX_SPEED = 1.5F;

    public static final float MIN_AIRSPEED = 0.12F;

    public static final float STALL_PITCH = -5.0F;
    public static final float STALL_SPEED = 0.25F;

    public static final float STALL_EXIT_SPEED = 0.4F;
    public static final int STALL_TICKS = 25;

    public static final float STALL_NOSE_DOWN = 30.0F;

    public static final float SOFT_LANDING_SPEED = 0.9F;

    public static final float HARD_LANDING_DAMAGE = 0.0F;

    public static final int STUMBLE_TICKS = 20;

    public static final float CRASH_SPEED = 0.5F;
    public static final float CRASH_DAMAGE = 8.0F;

    public static final float CRASH_SPEED_KEPT = 0.2F;

    public static final int FLAP_INTERVAL_CLIMB = 7;
    public static final int FLAP_INTERVAL_CRUISE = 14;
    public static final int FLAP_INTERVAL_GLIDE = 0;
    public static final int FLAP_INTERVAL_TAKEOFF = 5;
    public static final float FLAP_VOLUME = 0.7F;

    public static final float ROLL_PER_YAW_RATE = 4.0F;
    public static final float MAX_ROLL = 35.0F;

    public static final float ROLL_SMOOTHING = 0.15F;

    public static final float BODY_PITCH_FACTOR = 0.6F;

    public static final int TAKEOFF_ANIMATION_TICKS = 12;
    public static final int LANDING_ANIMATION_TICKS = 12;

    public static final float FOV_PER_SPEED = 0.15F;

    public static final int DEATH_SLOW_FALLING_TICKS = 8 * 20;

    public static final int DISMOUNT_SLOW_FALLING_TICKS = 3 * 20;

    public static final double BELLEROPHON_DISTANCE = 1000.0;

    public static final double ICARUS_ALTITUDE = 2000.0;
}
