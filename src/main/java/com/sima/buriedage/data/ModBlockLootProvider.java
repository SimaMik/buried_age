package com.sima.buriedage.data;

import java.util.List;
import java.util.Set;

import com.sima.buriedage.block.HephaestusForgeBlock;
import com.sima.buriedage.registry.ModBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.<Item>of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        this.add(ModBlocks.HEPHAESTUS_FORGE.get(), block ->
                this.createSinglePropConditionTable(block, HephaestusForgeBlock.HALF, DoubleBlockHalf.LOWER));
        this.dropSelf(ModBlocks.PEGASUS_EGG.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(ModBlocks.HEPHAESTUS_FORGE.get(), ModBlocks.PEGASUS_EGG.get());
    }
}
