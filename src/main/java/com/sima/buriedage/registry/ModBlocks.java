package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.block.BuildingMarkerBlock;
import com.sima.buriedage.block.HephaestusForgeBlock;
import com.sima.buriedage.block.PegasusEggBlock;
import com.sima.buriedage.block.MarkerBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TheBuriedAge.MODID);

    public static final DeferredBlock<HephaestusForgeBlock> HEPHAESTUS_FORGE = BLOCKS.registerBlock(
            "hephaestus_forge",
            HephaestusForgeBlock::new,
            properties -> properties
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.ANVIL)
                    .noOcclusion());

    public static final DeferredBlock<BuildingMarkerBlock> BUILDING_MARKER = BLOCKS.registerBlock(
            "building_marker", BuildingMarkerBlock::new, ModBlocks::markerProperties);

    public static final DeferredBlock<MarkerBlock> CAVITY_MARKER = marker("cavity_marker");

    public static final DeferredBlock<PegasusEggBlock> PEGASUS_EGG = BLOCKS.registerBlock(
            "pegasus_egg",
            PegasusEggBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    private static DeferredBlock<MarkerBlock> marker(String name) {
        return BLOCKS.registerBlock(name, MarkerBlock::new, ModBlocks::markerProperties);
    }

    private static BlockBehaviour.Properties markerProperties(BlockBehaviour.Properties properties) {
        return properties
                .noCollision()
                .noOcclusion()
                .noLootTable()
                .strength(-1.0F, 3600000.0F);
    }

    private ModBlocks() {}
}
