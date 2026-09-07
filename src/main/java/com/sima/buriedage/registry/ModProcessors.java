package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.worldgen.HephaestusForgeProcessor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModProcessors {
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, TheBuriedAge.MODID);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<HephaestusForgeProcessor>> HEPHAESTUS_FORGE =
            PROCESSORS.register("hephaestus_forge", () -> () -> HephaestusForgeProcessor.CODEC);

    private ModProcessors() {}
}
