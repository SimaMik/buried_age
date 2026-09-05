package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.advancement.BuildingDiscoveredTrigger;
import com.sima.buriedage.advancement.ForgeRitualTrigger;
import com.sima.buriedage.advancement.PegasusTrigger;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, TheBuriedAge.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, ForgeRitualTrigger> FORGE_RITUAL =
            TRIGGERS.register("forge_ritual", ForgeRitualTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, BuildingDiscoveredTrigger> BUILDING_DISCOVERED =
            TRIGGERS.register("building_discovered", BuildingDiscoveredTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, PegasusTrigger> PEGASUS =
            TRIGGERS.register("pegasus", PegasusTrigger::new);

    private ModTriggers() {}
}
