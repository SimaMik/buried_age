package com.sima.buriedage.client.pegasus;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.entity.PegasusEntity;
import com.sima.buriedage.entity.PegasusTuning;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The artist's GeckoLib model. The saddle and bridle are bones of the model wearing the vanilla
 * saddle pixels, shown by scaling them back from zero when a saddle is on; the foal is the same model at half size; in flight the whole body
 * pitches with the flight path and banks into turns on top of GeckoLib's own rotations.
 */
public class PegasusGeoRenderer extends GeoEntityRenderer<PegasusEntity, LivingEntityRenderState> {
    private static final DataTicket<Float> ROLL = DataTicket.create("buried_age_roll", Float.class);
    private static final DataTicket<Float> PITCH = DataTicket.create("buried_age_pitch", Float.class);
    private static final DataTicket<Boolean> SADDLED = DataTicket.create("buried_age_saddled", Boolean.class);
    private static final String SADDLE_BONE = "saddle";
    private static final String BRIDLE_BONE = "bridle";
    private static final float FOAL_SCALE = 0.5F;
    /** Height of the body centre in blocks: the point the flight pitch and bank rotate around. */
    private static final float TILT_PIVOT = 1.2F;

    public PegasusGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "pegasus")));
    }

    @Override
    public LivingEntityRenderState createRenderState(PegasusEntity animatable, @Nullable Void relatedObject) {
        return new LivingEntityRenderState();
    }

    @Override
    public void addRenderData(PegasusEntity pegasus, @Nullable Void relatedObject, LivingEntityRenderState state, float partialTick) {
        state.addGeckolibData(ROLL, pegasus.getRoll());
        state.addGeckolibData(PITCH, pegasus.getVisualPitch() * PegasusTuning.BODY_PITCH_FACTOR);
        state.addGeckolibData(SADDLED, pegasus.isSaddled());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, float widthScale, float heightScale) {
        float foal = renderPassInfo.renderState().isBaby ? FOAL_SCALE : 1.0F;
        super.scaleModelForRender(renderPassInfo, widthScale * foal, heightScale * foal);
    }

    @Override
    protected void applyRotations(RenderPassInfo<LivingEntityRenderState> renderPassInfo, PoseStack poseStack, float nativeScale) {
        super.applyRotations(renderPassInfo, poseStack, nativeScale);
        LivingEntityRenderState state = renderPassInfo.renderState();
        float pitch = state.getOrDefaultGeckolibData(PITCH, 0.0F);
        float roll = state.getOrDefaultGeckolibData(ROLL, 0.0F);
        if (pitch != 0.0F || roll != 0.0F) {
            poseStack.translate(0.0F, TILT_PIVOT, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
            poseStack.translate(0.0F, -TILT_PIVOT, 0.0F);
        }
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
        if (!renderPassInfo.renderState().getOrDefaultGeckolibData(SADDLED, false)) {
            snapshots.get(SADDLE_BONE).ifPresent(saddle -> saddle.setScale(0.0F, 0.0F, 0.0F));
            snapshots.get(BRIDLE_BONE).ifPresent(bridle -> bridle.setScale(0.0F, 0.0F, 0.0F));
        }
    }
}
