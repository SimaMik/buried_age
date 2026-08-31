package com.sima.buriedage.client.echo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sima.buriedage.entity.EchoEntity;
import com.sima.buriedage.entity.EchoTuning;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/**
 * Draws an echo as a washed-out, half-transparent villager. The body uses the plain villager
 * texture and the clothes are a second pass with the profession texture, both multiplied by the
 * same ghost tint so they fade together.
 */
public class EchoRenderer extends LivingEntityRenderer<EchoEntity, EchoRenderState, VillagerModel> {
    public EchoRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.0F);
        this.addLayer(new ProfessionLayer(this, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER_NO_HAT))));
    }

    @Override
    public EchoRenderState createRenderState() {
        return new EchoRenderState();
    }

    @Override
    public void extractRenderState(EchoEntity entity, EchoRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        int index = Math.floorMod(entity.getProfessionIndex(), EchoTuning.PROFESSIONS.length);
        state.professionTexture = EchoTuning.PROFESSIONS[index];
        state.opacity = entity.getOpacity();
    }

    @Override
    public Identifier getTextureLocation(EchoRenderState state) {
        return EchoTuning.BODY_TEXTURE;
    }

    /** Translucent so the alpha in the tint actually blends instead of being clipped. */
    @Override
    protected @Nullable RenderType getRenderType(EchoRenderState state, boolean isBodyVisible,
            boolean forceTransparent, boolean appearGlowing) {
        return RenderTypes.entityTranslucent(this.getTextureLocation(state));
    }

    @Override
    protected int getModelTint(EchoRenderState state) {
        return ghostTint(state.opacity);
    }

    /** Ghost colour with the fade folded into its alpha. */
    static int ghostTint(float opacity) {
        int base = EchoTuning.TINT_ARGB;
        int alpha = Math.round(ARGB.alpha(base) * Math.clamp(opacity, 0.0F, 1.0F));
        return ARGB.color(alpha, ARGB.red(base), ARGB.green(base), ARGB.blue(base));
    }

    /**
     * Second pass with the profession clothes. Not vanilla's VillagerProfessionLayer: that one
     * draws opaque, which would leave solid clothes floating on a transparent body.
     */
    private static class ProfessionLayer extends RenderLayer<EchoRenderState, VillagerModel> {
        private final VillagerModel clothes;

        ProfessionLayer(EchoRenderer parent, VillagerModel clothes) {
            super(parent);
            this.clothes = clothes;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                EchoRenderState state, float yRot, float xRot) {
            if (state.professionTexture == null || state.isInvisible) {
                return;
            }

            this.clothes.setupAnim(state);
            collector.submitModel(
                    this.clothes,
                    state,
                    poseStack,
                    RenderTypes.entityTranslucent(state.professionTexture),
                    lightCoords,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                    ghostTint(state.opacity),
                    null,
                    state.outlineColor,
                    null);
        }
    }
}
