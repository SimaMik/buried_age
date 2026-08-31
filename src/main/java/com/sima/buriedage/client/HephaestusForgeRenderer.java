package com.sima.buriedage.client;

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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HephaestusForgeRenderer implements BlockEntityRenderer<HephaestusForgeBlockEntity, HephaestusForgeRenderState> {
    /**
     * Top face of the anvil at the front of the model (elements at y 14.25..16.25, z 0.25..4.25).
     * The item being forged lies here.
     */
    private static final float ANVIL_TOP = 16.25F / 16.0F;
    private static final float ANVIL_CENTRE_X = 8.0F / 16.0F;
    private static final float ANVIL_CENTRE_Z = 2.25F / 16.0F;
    private static final float TARGET_SCALE = 0.32F;

    /**
     * The niche below the anvil, framed by the elements at x 5.5..10.5, y 1..6, z 0.25..5.75.
     * Its floor is the bar at y=1.5, and the block of diamond rests on that.
     */
    private static final float NICHE_CENTRE_X = 8.0F / 16.0F;
    private static final float NICHE_FLOOR_Y = 1.5F / 16.0F;
    private static final float NICHE_CENTRE_Z = 2.5F / 16.0F;
    /** Rendered edge length of the catalyst cube is 0.5 * this, because of the FIXED display scale. */
    private static final float CATALYST_SCALE = 0.42F;

    /**
     * The parchment panel on the back wall: element from (3,17,13.9) to (13,27,13.9). The glyphs
     * are drawn onto it, a hair in front so they never z-fight with the texture.
     */
    private static final float PARCHMENT_CENTRE_X = 8.0F / 16.0F;
    private static final float PARCHMENT_CENTRE_Y = 22.0F / 16.0F;
    private static final float PARCHMENT_Z = 13.9F / 16.0F - 0.005F;
    /** Font units to blocks. A five-letter line comes out about 0.4 blocks wide. */
    private static final float GLYPH_SCALE = 0.013F;
    /** The enchanting table alphabet. */
    private static final FontDescription GALACTIC = new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));
    /** Half-transparent warm ink, so the glyphs read as faded engraving rather than a label. */
    private static final int GLYPH_COLOR = 0x70FFE9B0;
    private static final int MAX_GLYPH_LINES = 3;

    private final ItemModelResolver itemModelResolver;
    private final Font font;

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
        this.itemModelResolver.updateForTopItem(state.blueprint, blockEntity.getBlueprint(),
                ItemDisplayContext.FIXED, blockEntity.getLevel(), null, seed + 1);
        this.itemModelResolver.updateForTopItem(state.catalyst, blockEntity.getCatalyst(),
                ItemDisplayContext.FIXED, blockEntity.getLevel(), null, seed + 2);

        state.glyphs.clear();
        Holder<Enchantment> carried = AncientBlueprintItem.getEnchantment(blockEntity.getBlueprint());
        if (carried != null) {
            for (String chunk : glyphLines(carried)) {
                state.glyphs.add(Component.literal(chunk)
                        .withStyle(style -> style.withFont(GALACTIC))
                        .getVisualOrderText());
            }
        }
    }

    /** Chops the enchantment id into a few short runs so the glyphs look like written lines. */
    private static List<String> glyphLines(Holder<Enchantment> enchantment) {
        String source = enchantment.getRegisteredName().replaceAll("^.*:", "").replaceAll("[^a-z]", "");
        List<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < source.length() && lines.size() < MAX_GLYPH_LINES; i += 5) {
            lines.add(source.substring(i, Math.min(i + 5, source.length())));
        }

        return lines;
    }

    @Override
    public void submit(HephaestusForgeRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        // The blueprint is no longer an item lying on the bench: it shows as the parchment texture
        // on the back wall, so only the work and the catalyst occupy the surface.
        // The work lies flat on the anvil itself.
        if (!state.target.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(ANVIL_CENTRE_X, ANVIL_TOP, ANVIL_CENTRE_Z);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(TARGET_SCALE, TARGET_SCALE, TARGET_SCALE);
            state.target.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        // The block of diamond sits in the niche underneath. A block item is a cube, and block/block
        // bakes a 0.5 scale into its FIXED display, so half its rendered height is 0.25 * the scale:
        // lift it by exactly that and it rests on the niche floor instead of sinking through it.
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
        // Onto the parchment panel on the wall, facing the player who walks up to the forge.
        poseStack.translate(PARCHMENT_CENTRE_X, PARCHMENT_CENTRE_Y, PARCHMENT_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // Font space grows downward, hence the negative Y scale.
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
