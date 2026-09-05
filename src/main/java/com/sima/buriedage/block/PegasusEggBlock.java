package com.sima.buriedage.block;

import java.util.UUID;

import com.mojang.serialization.MapCodec;
import com.sima.buriedage.block.entity.PegasusEggBlockEntity;
import com.sima.buriedage.entity.PegasusEntity;
import com.sima.buriedage.entity.PegasusTuning;
import com.sima.buriedage.registry.ModEntities;
import com.sima.buriedage.registry.ModTriggers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Hatches on a timer wherever it is set down, in three cracking stages like the sniffer egg. The
 * block entity remembers who placed it, so the foal belongs to that player from its first breath.
 */
public class PegasusEggBlock extends Block implements EntityBlock {
    public static final MapCodec<PegasusEggBlock> CODEC = simpleCodec(PegasusEggBlock::new);
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
    private static final int STAGES = 3;
    private static final VoxelShape SHAPE = Block.column(14.0, 12.0, 0.0, 16.0);

    public PegasusEggBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0));
    }

    @Override
    protected MapCodec<PegasusEggBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PegasusEggBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (by instanceof Player player && level.getBlockEntity(pos) instanceof PegasusEggBlockEntity egg) {
            egg.setOwner(player.getUUID());
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(state));
        if (PegasusTuning.HATCHING_ENABLED) {
            level.scheduleTick(pos, this, this.stageDelay(level.getRandom()));
        }
    }

    private int stageDelay(RandomSource random) {
        return PegasusTuning.HATCH_TICKS / STAGES + random.nextInt(Math.max(1, PegasusTuning.HATCH_RANDOM_TICKS / STAGES));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!PegasusTuning.HATCHING_ENABLED) {
            return;
        }
        int stage = state.getValue(HATCH);
        if (stage < STAGES - 1) {
            level.playSound(null, pos, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, stage + 1), 2);
            level.scheduleTick(pos, this, this.stageDelay(random));
            return;
        }
        this.hatch(level, pos);
    }

    private void hatch(ServerLevel level, BlockPos pos) {
        ServerPlayer owner = null;
        UUID placedBy = level.getBlockEntity(pos) instanceof PegasusEggBlockEntity egg ? egg.getOwner() : null;
        if (placedBy != null && level.getPlayerByUUID(placedBy) instanceof ServerPlayer online) {
            owner = online;
        }
        if (owner == null && placedBy == null) {
            Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16.0, false);
            if (nearest instanceof ServerPlayer near) {
                owner = near;
            }
        }

        level.playSound(null, pos, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        level.playSound(null, pos, SoundEvents.HORSE_AMBIENT_BABY, SoundSource.NEUTRAL, 0.8F, 1.0F);
        Vec3 center = pos.getCenter();
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 20, 0.4, 0.3, 0.4, 0.02);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 0.3, center.z, 12, 0.5, 0.4, 0.5, 0.0);
        level.destroyBlock(pos, false);

        PegasusEntity foal = ModEntities.PEGASUS.get().create(level, EntitySpawnReason.BREEDING);
        if (foal == null) {
            return;
        }
        foal.snapTo(center.x, pos.getY(), center.z, Mth.wrapDegrees(level.getRandom().nextFloat() * 360.0F), 0.0F);
        foal.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.BREEDING, null);
        foal.setBaby(true);
        foal.setPersistenceRequired();
        if (owner != null) {
            foal.setOwner(owner);
            foal.setTamed(true);
        } else if (placedBy != null) {
            foal.setPendingOwner(placedBy);
        }
        level.addFreshEntity(foal);
        if (owner != null) {
            ModTriggers.PEGASUS.get().trigger(owner, "hatched");
        }
    }
}
