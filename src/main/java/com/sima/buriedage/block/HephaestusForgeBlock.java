package com.sima.buriedage.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import com.sima.buriedage.block.entity.HephaestusForgeBlockEntity;
import com.sima.buriedage.item.AncientBlueprintItem;
import com.sima.buriedage.item.HephaestusHammerItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class HephaestusForgeBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<HephaestusForgeBlock> CODEC = simpleCodec(HephaestusForgeBlock::new);

    public static final BooleanProperty HAS_BLUEPRINT = BooleanProperty.create("has_blueprint");
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape LOWER_SHAPE = Shapes.block();
    private static final Map<Direction, VoxelShape> UPPER_SHAPES =
            Shapes.rotateHorizontal(Block.box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0));

    public HephaestusForgeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(HAS_BLUEPRINT, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, HAS_BLUEPRINT, FACING);
    }

    @Override
    protected MapCodec<HephaestusForgeBlock> codec() {
        return CODEC;
    }

    public static boolean isLower(BlockState state) {
        return state.getBlock() instanceof HephaestusForgeBlock && state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    public static boolean isUpper(BlockState state) {
        return state.getBlock() instanceof HephaestusForgeBlock && state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER_SHAPES.get(state.getValue(FACING)) : LOWER_SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new HephaestusForgeBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER || isLower(level.getBlockState(pos.below()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN) {
            return isLower(neighbourState) ? neighbourState.setValue(HALF, DoubleBlockHalf.UPPER)
                    : Blocks.AIR.defaultBlockState();
        }

        if (half == DoubleBlockHalf.LOWER && !isUpper(level.getBlockState(pos.above()))) {
            ticks.scheduleTick(pos, this, 1);
        }

        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return;
        }

        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (!isUpper(aboveState) && aboveState.canBeReplaced()) {
            level.setBlock(above, state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (isLower(lower)) {
                boolean drops = !player.isCreative() && player.hasCorrectToolForDrops(lower, level, below);
                level.destroyBlock(below, drops, player);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof HephaestusForgeBlockEntity forge && forge.isCharged()) {
            level.addParticle(ParticleTypes.SMALL_FLAME,
                    pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.85,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                    0.0, 0.02, 0.0);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (!(level.getBlockEntity(base) instanceof HephaestusForgeBlockEntity forge)) {
            return InteractionResult.PASS;
        }

        if (stack.isEmpty()) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            return forge.returnLastInserted(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (stack.getItem() instanceof HephaestusHammerItem) {
            if (level instanceof ServerLevel serverLevel) {
                forge.strike(serverLevel, base, player);
            }

            return InteractionResult.SUCCESS;
        }

        int slot = slotFor(stack);
        if (slot < 0) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        return forge.insert(slot, stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static int slotFor(ItemStack stack) {
        if (stack.getItem() instanceof AncientBlueprintItem) {
            return HephaestusForgeBlockEntity.SLOT_BLUEPRINT;
        }

        if (HephaestusForgeBlockEntity.isCatalyst(stack)) {
            return HephaestusForgeBlockEntity.SLOT_CATALYST;
        }

        if (stack.has(DataComponents.ENCHANTABLE)) {
            return HephaestusForgeBlockEntity.SLOT_TARGET;
        }

        return -1;
    }
}
