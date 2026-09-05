package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.block.entity.BuildingMarkerBlockEntity;
import com.sima.buriedage.block.entity.HephaestusForgeBlockEntity;
import com.sima.buriedage.block.entity.PegasusEggBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheBuriedAge.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HephaestusForgeBlockEntity>> HEPHAESTUS_FORGE =
            BLOCK_ENTITIES.register("hephaestus_forge",
                    () -> new BlockEntityType<>(HephaestusForgeBlockEntity::new, ModBlocks.HEPHAESTUS_FORGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BuildingMarkerBlockEntity>> BUILDING_MARKER =
            BLOCK_ENTITIES.register("building_marker",
                    () -> new BlockEntityType<>(BuildingMarkerBlockEntity::new, ModBlocks.BUILDING_MARKER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PegasusEggBlockEntity>> PEGASUS_EGG =
            BLOCK_ENTITIES.register("pegasus_egg",
                    () -> new BlockEntityType<>(PegasusEggBlockEntity::new, ModBlocks.PEGASUS_EGG.get()));

    private ModBlockEntities() {}
}
