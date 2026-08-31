package com.sima.buriedage.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sima.buriedage.block.entity.HephaestusForgeBlockEntity;
import com.sima.buriedage.item.AncientBlueprintItem;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HephaestusForgeRenderer implements BlockEntityRenderer<HephaestusForgeBlockEntity, HephaestusForgeRenderState> {
    private static final float ANVIL_TOP = 16.25F / 16.0F;
    private static final float ANVIL_CENTRE_X = 8.0F / 16.0F;
    private static final float ANVIL_CENTRE_Z = 2.25F / 16.0F;
    private static final float TARGET_SCALE = 0.32F;

    private static final float NICHE_CENTRE_X = 8.0F / 16.0F;
    private static final float NICHE_FLOOR_Y = 1.5F / 16.0F;
    private static final float NICHE_CENTRE_Z = 2.5F / 16.0F;
    private static final float CATALYST_SCALE = 0.42F;

    private static final float PARCHMENT_CENTRE_X = 8.0F / 16.0F;
    private static final float PARCHMENT_CENTRE_Y = 22.0F / 16.0F;
    private static final float PARCHMENT_Z = 13.9F / 16.0F - 0.005F;
    private static final float GLYPH_SCALE = 0.013F;
    private static final FontDescription GALACTIC = new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));
    private static final int GLYPH_COLOR = 0x70FFE9B0;
    private static final int MAX_GLYPH_LINES = 3;

    private static final int GLYPHS_PER_LINE = 5;

    private final ItemModelResolver itemModelResolver;
    private final Font font;
    private @Nullable Holder<Enchantment> cachedFor;
    private List<FormattedCharSequence> cachedGlyphs = List.of();

    public HephaestusForgeRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.font = context.font();
    }

    @Override
    public HephaestusForgeRenderState createRenderState() {
        return new HephaestusForgeRenderState();
    }

    @Override
    public void extractRenderState(HephaestusForgeBlockEntity blockEntity, HephaestusForgeRenderState state,
            float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        int seed = (int) blockEntity.getBlockPos().asLong();
        this.itemModelResolver.updateForTopItem(state.target, blockEntity.getTarget(),
                ItemDisplayContext.FIXED, blockEntity.getLevel(), null, seed);
        this.itemModelResolver.updateForTopItem(state.catalyst, blockEntity.getCatalyst(),
                ItemDisplayContext.FIXED, blockEntity.getLevel(), null, seed + 2);

        Holder<Enchantment> carried = AncientBlueprintItem.getEnchantment(blockEntity.getBlueprint());
        if (carried != this.cachedFor) {
            this.cachedFor = carried;
            this.cachedGlyphs = carried == null ? List.of() : shapeGlyphs(carried);
        }

        state.glyphs = this.cachedGlyphs;
    }

    private static List<FormattedCharSequence> shapeGlyphs(Holder<Enchantment> enchantment) {
        String path = enchantment.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
        List<FormattedCharSequence> lines = new ArrayList<>(MAX_GLYPH_LINES);
        for (int i = 0; i < path.length() && lines.size() < MAX_GLYPH_LINES; i += GLYPHS_PER_LINE) {
            String chunk = path.substring(i, Math.min(i + GLYPHS_PER_LINE, path.length())).replace('_', ' ');
            lines.add(Component.literal(chunk)
                    .withStyle(style -> style.withFont(GALACTIC))
                    .getVisualOrderText());
        }

        return lines;
    }

    @Override
    public void submit(HephaestusForgeRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (!state.target.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(ANVIL_CENTRE_X, ANVIL_TOP, ANVIL_CENTRE_Z);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(TARGET_SCALE, TARGET_SCALE, TARGET_SCALE);
            state.target.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        if (!state.catalyst.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(NICHE_CENTRE_X, NICHE_FLOOR_Y + 0.25F * CATALYST_SCALE, NICHE_CENTRE_Z);
            poseStack.scale(CATALYST_SCALE, CATALYST_SCALE, CATALYST_SCALE);
            state.catalyst.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        this.submitGlyphs(state, poseStack, collector);
    }

    private void submitGlyphs(HephaestusForgeRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.glyphs.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(PARCHMENT_CENTRE_X, PARCHMENT_CENTRE_Y, PARCHMENT_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(GLYPH_SCALE, -GLYPH_SCALE, GLYPH_SCALE);

        int lineHeight = 9;
        float firstLineY = -(state.glyphs.size() * lineHeight) / 2.0F;
        for (int line = 0; line < state.glyphs.size(); line++) {
            var text = state.glyphs.get(line);
            float x = -this.font.width(text) / 2.0F;
            collector.submitText(poseStack, x, firstLineY + line * lineHeight, text, false,
                    Font.DisplayMode.POLYGON_OFFSET, state.lightCoords, GLYPH_COLOR, 0, 0);
        }

        poseStack.popPose();
    }
}
