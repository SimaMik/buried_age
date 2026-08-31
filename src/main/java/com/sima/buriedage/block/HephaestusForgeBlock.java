package com.sima.buriedage.block;

import com.mojang.serialization.MapCodec;
import com.sima.buriedage.block.entity.HephaestusForgeBlockEntity;
import com.sima.buriedage.item.AncientBlueprintItem;
import com.sima.buriedage.item.HephaestusHammerItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class HephaestusForgeBlock extends Block implements EntityBlock {
    public static final MapCodec<HephaestusForgeBlock> CODEC = simpleCodec(HephaestusForgeBlock::new);

    /**
     * The model only knows how to show two things: a bare stele, and a stele with a scroll
     * baked onto it. That's the only axis the artist actually modeled, so that's the only axis
     * the blockstate needs. Whether the forge is "ready to strike" (all three slots full) is
     * checked straight off the block entity in animateTick instead of being its own state.
     */
    public static final BooleanProperty HAS_BLUEPRINT = BooleanProperty.create("has_blueprint");

    /** Placeholder silhouette until the real model's collision shape is measured in-game. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public HephaestusForgeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_BLUEPRINT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_BLUEPRINT);
    }

    @Override
    protected MapCodec<HephaestusForgeBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HephaestusForgeBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Sparks are about "the ritual can fire right now", not about the blueprint being
        // pinned to the stele — so this reads the block entity directly instead of the
        // has_blueprint state.
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
        if (!(level.getBlockEntity(pos) instanceof HephaestusForgeBlockEntity forge)) {
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
                forge.strike(serverLevel, pos, player);
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

    /** Which slot this item belongs in, or -1 if the forge has no use for it. */
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