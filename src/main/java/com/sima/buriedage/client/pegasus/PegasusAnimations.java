package com.sima.buriedage.client.pegasus;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * The seven wing animations, as placeholders. Each one is what Blockbench exports for a vanilla
 * {@code AnimationDefinition}: replace the body of a constant with the exported builder chain and
 * keep the bone names ({@code left_wing}, {@code right_wing}, {@code body}, {@code head_parts},
 * the four legs, {@code tail}).
 *
 * <p>Rotations are offsets on top of the model's rest pose, in which the wings are folded up
 * along the body. A positive Z offset on the left wing (negative on the right) opens it.
 */
public final class PegasusAnimations {
    private PegasusAnimations() {}

    private static final AnimationChannel.Interpolation LINEAR = AnimationChannel.Interpolations.LINEAR;
    private static final AnimationChannel.Interpolation SMOOTH = AnimationChannel.Interpolations.CATMULLROM;

    /** Standing still: wings folded, a faint breath. */
    public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(3.0F).looping()
            .addAnimation("left_wing", wing(SMOOTH, 0.0F, 0.0F, 1.5F, 3.0F, 3.0F, 0.0F))
            .addAnimation("right_wing", wing(SMOOTH, 0.0F, 0.0F, 1.5F, -3.0F, 3.0F, 0.0F))
            .build();

    /** Walking: folded wings sway with the step. */
    public static final AnimationDefinition WALK = AnimationDefinition.Builder.withLength(1.0F).looping()
            .addAnimation("left_wing", wing(LINEAR, 0.0F, 0.0F, 0.5F, 6.0F, 1.0F, 0.0F))
            .addAnimation("right_wing", wing(LINEAR, 0.0F, 0.0F, 0.5F, -6.0F, 1.0F, 0.0F))
            .build();

    /** Galloping: wings half open and beating lightly with the stride. */
    public static final AnimationDefinition GALLOP = AnimationDefinition.Builder.withLength(0.5F).looping()
            .addAnimation("left_wing", wing(LINEAR, 0.0F, 20.0F, 0.25F, 35.0F, 0.5F, 20.0F))
            .addAnimation("right_wing", wing(LINEAR, 0.0F, -20.0F, 0.25F, -35.0F, 0.5F, -20.0F))
            .build();

    /** Takeoff: two deep, fast strokes that end with the wings spread. */
    public static final AnimationDefinition TAKEOFF = AnimationDefinition.Builder.withLength(0.6F)
            .addAnimation("left_wing", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), LINEAR),
                    new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 110.0F), LINEAR),
                    new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), LINEAR),
                    new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 110.0F), LINEAR),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 65.0F), LINEAR)))
            .addAnimation("right_wing", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), LINEAR),
                    new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -110.0F), LINEAR),
                    new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), LINEAR),
                    new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -110.0F), LINEAR),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -65.0F), LINEAR)))
            .build();

    /** Powered flight: a steady beat between an up-stroke and a down-stroke. */
    public static final AnimationDefinition FLY = AnimationDefinition.Builder.withLength(0.7F).looping()
            .addAnimation("left_wing", wing(SMOOTH, 0.0F, 45.0F, 0.35F, 100.0F, 0.7F, 45.0F))
            .addAnimation("right_wing", wing(SMOOTH, 0.0F, -45.0F, 0.35F, -100.0F, 0.7F, -45.0F))
            .build();

    /** Gliding: wings spread and almost still. */
    public static final AnimationDefinition GLIDE = AnimationDefinition.Builder.withLength(3.0F).looping()
            .addAnimation("left_wing", wing(SMOOTH, 0.0F, 65.0F, 1.5F, 70.0F, 3.0F, 65.0F))
            .addAnimation("right_wing", wing(SMOOTH, 0.0F, -65.0F, 1.5F, -70.0F, 3.0F, -65.0F))
            .build();

    /** Landing: one braking stroke, then the wings fold. */
    public static final AnimationDefinition LAND = AnimationDefinition.Builder.withLength(0.6F)
            .addAnimation("left_wing", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 65.0F), LINEAR),
                    new Keyframe(0.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 95.0F), LINEAR),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), LINEAR)))
            .addAnimation("right_wing", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -65.0F), LINEAR),
                    new Keyframe(0.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -95.0F), LINEAR),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), LINEAR)))
            .build();

    /** A three-key Z rotation channel: start, middle, end. */
    private static AnimationChannel wing(AnimationChannel.Interpolation interpolation,
                                         float t0, float z0, float t1, float z1, float t2, float z2) {
        return new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(t0, KeyframeAnimations.degreeVec(0.0F, 0.0F, z0), interpolation),
                new Keyframe(t1, KeyframeAnimations.degreeVec(0.0F, 0.0F, z1), interpolation),
                new Keyframe(t2, KeyframeAnimations.degreeVec(0.0F, 0.0F, z2), interpolation));
    }
}
