package com.sima.buriedage.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

public final class EchoTuning {

    public static final boolean AUTO_SPAWN_ENABLED = true;

    public static final int COOLDOWN_MIN = 75 * 20;
    public static final int COOLDOWN_MAX = 120 * 20;

    public static final int ROLL_INTERVAL = 15 * 20;

    public static final float SPAWN_CHANCE = 0.40F;

    public static final float DOUBLE_SPAWN_CHANCE = 0.25F;

    public static final int MAX_ECHOES = 4;

    public static final double SPAWN_RADIUS_MIN = 4.0;
    public static final double SPAWN_RADIUS_MAX = 12.0;

    public static final int SPAWN_VERTICAL_SEARCH = 4;

    public static final int WEIGHT_DRIFT = 45;
    public static final int WEIGHT_WANDER = 35;
    public static final int WEIGHT_MEETING = 20;

    public static final int FADE_IN_TICKS = 10;
    public static final int FADE_TICKS = 40;

    public static final double BOB_SPEED = 0.12;

    public static final int DISSOLVE_PARTICLES = 12;

    public static final float WHISPER_VOLUME = 0.35F;
    public static final float WHISPER_PITCH = 0.6F;

    public static final int DRIFT_LIFETIME_MIN = 6 * 20;
    public static final int DRIFT_LIFETIME_MAX = 8 * 20;
    public static final float DRIFT_SPEED = 0.022F;
    public static final float DRIFT_BOB = 0.004F;

    public static final int WANDER_LIFETIME_MIN = 12 * 20;
    public static final int WANDER_LIFETIME_MAX = 18 * 20;
    public static final float WANDER_SPEED = 0.035F;
    public static final float WANDER_BOB = 0.006F;
    public static final float WANDER_PAUSE_BOB = 0.010F;

    public static final int WANDER_STEPS_MIN = 2;
    public static final int WANDER_STEPS_MAX = 4;

    public static final int WANDER_LEG_BLOCKS_MIN = 3;
    public static final int WANDER_LEG_BLOCKS_MAX = 6;

    public static final int WANDER_PAUSE_MIN = 2 * 20;
    public static final int WANDER_PAUSE_MAX = 3 * 20;

    public static final int WANDER_GLANCE_INTERVAL = 12;
    public static final float WANDER_GLANCE_SPREAD = 90.0F;

    public static final float WANDER_TURN_SPREAD = 220.0F;

    public static final int MEETING_LIFETIME_MIN = 15 * 20;
    public static final int MEETING_LIFETIME_MAX = 20 * 20;
    public static final float MEETING_SPEED = 0.030F;
    public static final float MEETING_BOB = 0.006F;
    public static final float MEETING_TALK_BOB = 0.012F;

    public static final double MEETING_SEPARATION_MIN = 6.0;
    public static final double MEETING_SEPARATION_MAX = 10.0;

    public static final double MEETING_ARRIVAL_RADIUS_SQR = 1.2 * 1.2;

    public static final int MEETING_TALK_MIN = 4 * 20;
    public static final int MEETING_TALK_MAX = 6 * 20;

    public static final Identifier[] PROFESSIONS = {
            profession("farmer"),
            profession("armorer"),
            profession("shepherd"),
            profession("mason"),
            profession("librarian"),
            profession("nitwit"),
    };

    public static final Identifier BODY_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    public static final int TINT_ARGB = 0x8FA8C4D8;

    private static Identifier profession(String name) {
        return Identifier.withDefaultNamespace("textures/entity/villager/profession/" + name + ".png");
    }

    public static int randomBetween(RandomSource random, int min, int max) {
        return max <= min ? min : min + random.nextInt(max - min + 1);
    }

    public static double randomBetween(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private EchoTuning() {}
}
