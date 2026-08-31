package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.advancement.ForgeRitualTrigger;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, TheBuriedAge.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, ForgeRitualTrigger> FORGE_RITUAL =
            TRIGGERS.register("forge_ritual", ForgeRitualTrigger::new);

    private ModTriggers() {}
}
