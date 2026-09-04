package com.sima.buriedage.client;

import java.util.List;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.FormattedCharSequence;

public class HephaestusForgeRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState target = new ItemStackRenderState();
    public final ItemStackRenderState catalyst = new ItemStackRenderState();
    public List<FormattedCharSequence> glyphs = List.of();
    public float facingAngle;
}
