package com.sima.buriedage.client.pegasus;

import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.AnimationState;

public class PegasusRenderState extends EquineRenderState {
    public final AnimationState idle = new AnimationState();
    public final AnimationState walk = new AnimationState();
    public final AnimationState gallop = new AnimationState();
    public final AnimationState takeoff = new AnimationState();
    public final AnimationState fly = new AnimationState();
    public final AnimationState glide = new AnimationState();
    public final AnimationState land = new AnimationState();

    /** Bank angle, degrees, positive when turning right. */
    public float roll;
    /** Body tilt along the flight path, degrees, positive nose down. */
    public float pitch;
}
