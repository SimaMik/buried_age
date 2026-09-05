package com.sima.buriedage.client.pegasus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sima.buriedage.entity.PegasusEntity;
import com.sima.buriedage.entity.PegasusTuning;

import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;

/** The horse renderer with the wing model, vanilla saddle and armor layers, and a banking, pitching body in flight. */
public class PegasusRenderer extends AbstractHorseRenderer<PegasusEntity, PegasusRenderState, PegasusModel> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/horse/horse_white.png");
    private static final Identifier BABY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/horse/horse_white_baby.png");

    public PegasusRenderer(EntityRendererProvider.Context context) {
        super(context, new PegasusModel(context.bakeLayer(PegasusModel.LAYER)), new PegasusModel(context.bakeLayer(PegasusModel.BABY_LAYER)));
        this.addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_BODY,
                state -> state.bodyArmorItem, new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR)), null, 2));
        this.addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_SADDLE,
                state -> state.saddle, new EquineSaddleModel(context.bakeLayer(ModelLayers.HORSE_SADDLE)), null, 2));
    }

    @Override
    public Identifier getTextureLocation(PegasusRenderState state) {
        return state.isBaby ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    public PegasusRenderState createRenderState() {
        return new PegasusRenderState();
    }

    @Override
    public void extractRenderState(PegasusEntity entity, PegasusRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.idle.copyFrom(entity.idleAnimation);
        state.walk.copyFrom(entity.walkAnimation);
        state.gallop.copyFrom(entity.gallopAnimation);
        state.takeoff.copyFrom(entity.takeoffAnimation);
        state.fly.copyFrom(entity.flyAnimation);
        state.glide.copyFrom(entity.glideAnimation);
        state.land.copyFrom(entity.landAnimation);
        state.roll = entity.getRoll();
        state.pitch = entity.getVisualPitch() * PegasusTuning.BODY_PITCH_FACTOR;
    }

    @Override
    protected void setupRotations(PegasusRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (state.pitch != 0.0F || state.roll != 0.0F) {
            float pivot = state.boundingBoxHeight * 0.6F;
            poseStack.translate(0.0F, pivot, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.roll));
            poseStack.translate(0.0F, -pivot, 0.0F);
        }
    }
}
