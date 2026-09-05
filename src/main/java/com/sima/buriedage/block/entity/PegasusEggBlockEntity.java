package com.sima.buriedage.block.entity;

import java.util.UUID;

import com.sima.buriedage.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Only remembers who set the egg down, so the foal can belong to them. */
public class PegasusEggBlockEntity extends BlockEntity {
    private @Nullable UUID owner;

    public PegasusEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PEGASUS_EGG.get(), pos, state);
    }

    public @Nullable UUID getOwner() {
        return this.owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.owner != null) {
            output.store("owner", UUIDUtil.CODEC, this.owner);
        }
    }
}
