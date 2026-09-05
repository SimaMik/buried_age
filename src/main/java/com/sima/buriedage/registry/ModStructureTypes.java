package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.worldgen.BuriedCityStructure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, TheBuriedAge.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<BuriedCityStructure>> BURIED_CITY =
            STRUCTURE_TYPES.register("buried_city", () -> () -> BuriedCityStructure.CODEC);

    private ModStructureTypes() {}
}
