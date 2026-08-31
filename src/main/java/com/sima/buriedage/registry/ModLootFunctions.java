package com.sima.buriedage.registry;

import com.mojang.serialization.MapCodec;
import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.loot.RandomBlueprintFunction;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModLootFunctions {
    public static final DeferredRegister<MapCodec<? extends LootItemFunction>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, TheBuriedAge.MODID);

    public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<RandomBlueprintFunction>> RANDOM_BLUEPRINT =
            LOOT_FUNCTIONS.register("random_blueprint", () -> RandomBlueprintFunction.MAP_CODEC);

    private ModLootFunctions() {}
}
