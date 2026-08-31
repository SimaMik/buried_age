package com.sima.buriedage.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.FormattedCharSequence;

public class HephaestusForgeRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState target = new ItemStackRenderState();
    public final ItemStackRenderState blueprint = new ItemStackRenderState();
    public final ItemStackRenderState catalyst = new ItemStackRenderState();
    /** Standard galactic glyphs drawn over the blueprint; empty when no blueprint is inserted. */
    public final List<FormattedCharSequence> glyphs = new ArrayList<>();
}
