package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.block.HephaestusForgeBlock;
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

    /** Marks the temple cella so the location advancement can fire. Stays in the world. */
    public static final DeferredBlock<MarkerBlock> CELLA_MARKER = marker("cella_marker");

    /** Marks a room that must stay a real cavity. The structure processor turns it back into air. */
    public static final DeferredBlock<MarkerBlock> CAVITY_MARKER = marker("cavity_marker");

    private static DeferredBlock<MarkerBlock> marker(String name) {
        return BLOCKS.registerBlock(name, MarkerBlock::new, properties -> properties
                .noCollision()
                .noOcclusion()
                .noLootTable()
                .strength(-1.0F, 3600000.0F));
    }

    private ModBlocks() {}
}
