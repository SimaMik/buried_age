package com.sima.buriedage.client.pegasus;

import com.sima.buriedage.TheBuriedAge;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.BabyHorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * The vanilla horse with two flat wings hung on the body. A placeholder until the artist's model
 * arrives: swap the meshes here, keep the bone names, and the animations keep working.
 */
public class PegasusModel extends AbstractEquineModel<PegasusRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "pegasus"), "main");
    public static final ModelLayerLocation BABY_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "pegasus_baby"), "main");

    /** Rest pose of a wing: folded up along the flank. Animations open it from here. */
    private static final float FOLDED = (float) Math.toRadians(75.0);

    private final KeyframeAnimation idle;
    private final KeyframeAnimation walk;
    private final KeyframeAnimation gallop;
    private final KeyframeAnimation takeoff;
    private final KeyframeAnimation fly;
    private final KeyframeAnimation glide;
    private final KeyframeAnimation land;

    public PegasusModel(ModelPart root) {
        super(root);
        this.idle = PegasusAnimations.IDLE.bake(root);
        this.walk = PegasusAnimations.WALK.bake(root);
        this.gallop = PegasusAnimations.GALLOP.bake(root);
        this.takeoff = PegasusAnimations.TAKEOFF.bake(root);
        this.fly = PegasusAnimations.FLY.bake(root);
        this.glide = PegasusAnimations.GLIDE.bake(root);
        this.land = PegasusAnimations.LAND.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = createBodyMesh(CubeDeformation.NONE);
        addWings(mesh, 16.0F, 10.0F, 5.0F, -7.0F, -9.0F);
        return LayerDefinition.create(mesh, 64, 64).apply(MeshTransformer.scaling(1.1F));
    }

    public static LayerDefinition createBabyLayer() {
        MeshDefinition mesh = BabyHorseModel.createBabyMesh(CubeDeformation.NONE);
        addWings(mesh, 10.0F, 7.0F, 4.0F, -3.0F, -3.0F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addWings(MeshDefinition mesh, float length, float chord, float x, float y, float z) {
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 32).addBox(0.0F, -0.5F, -chord / 2.0F, length, 1.0F, chord),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, -FOLDED));
        body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-length, -0.5F, -chord / 2.0F, length, 1.0F, chord),
                PartPose.offsetAndRotation(-x, y, z, 0.0F, 0.0F, FOLDED));
    }

    @Override
    public void setupAnim(PegasusRenderState state) {
        super.setupAnim(state);
        this.idle.apply(state.idle, state.ageInTicks);
        this.walk.apply(state.walk, state.ageInTicks);
        this.gallop.apply(state.gallop, state.ageInTicks);
        this.takeoff.apply(state.takeoff, state.ageInTicks);
        this.fly.apply(state.fly, state.ageInTicks);
        this.glide.apply(state.glide, state.ageInTicks);
        this.land.apply(state.land, state.ageInTicks);
    }
}
