package com.sima.buriedage.block;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.sima.buriedage.block.entity.BuildingMarkerBlockEntity;
import com.sima.buriedage.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BuildingMarkerBlock extends MarkerBlock implements EntityBlock {
    public static final MapCodec<BuildingMarkerBlock> CODEC = simpleCodec(BuildingMarkerBlock::new);
    public static final BooleanProperty BURIED = BooleanProperty.create("buried");
    private static final float BURIED_HARDNESS = 0.6F;

    public BuildingMarkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BURIED, false));
    }

    @Override
    protected MapCodec<? extends MarkerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BURIED);
    }

    private static boolean buried(BlockState state) {
        return state.getValue(BURIED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return buried(state) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return buried(state) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return buried(state) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return buried(state) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return buried(state) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return !buried(state);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return buried(state) ? 0.2F : 1.0F;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!buried(state)) {
            return 0.0F;
        }
        return player.getDestroySpeed(state) / BURIED_HARDNESS / 30.0F;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return buried(state) ? List.of(new ItemStack(Items.GRAVEL)) : List.of();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BuildingMarkerBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.BUILDING_MARKER.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<BuildingMarkerBlockEntity>)
                (lvl, pos, st, marker) -> marker.serverTick((ServerLevel) lvl);
    }
}
