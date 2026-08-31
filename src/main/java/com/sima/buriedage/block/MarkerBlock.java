package com.sima.buriedage.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible, intangible build-time marker, shaped after vanilla's light block: no collision, no
 * outline, no crosshair target, unbreakable — except while the player is holding the matching
 * marker item, when it turns into a normal selectable cube so it can be found and removed again.
 *
 * <p>Two of them exist:
 * <ul>
 *   <li>{@code cella_marker} stays in the world and lets the location advancement fire. That
 *       criterion reads the block at the player's feet position, not a raycast, so an empty
 *       collision shape does not affect it;</li>
 *   <li>{@code cavity_marker} is turned back into air by the structure processor, and only exists
 *       so a builder can tell "this room is a real cavity" from "this is solid rock".</li>
 * </ul>
 */
public class MarkerBlock extends Block {
    public static final MapCodec<MarkerBlock> CODEC = simpleCodec(MarkerBlock::new);

    public MarkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<MarkerBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Same trick the light block uses: invisible to the crosshair unless you are holding one.
        return context.isHoldingItem(this.asItem()) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
