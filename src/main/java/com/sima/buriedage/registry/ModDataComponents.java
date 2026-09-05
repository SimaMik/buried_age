package com.sima.buriedage.registry;

import com.mojang.serialization.Codec;
import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.journal.BuildingMarker;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TheBuriedAge.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Holder<Enchantment>>> BLUEPRINT_ENCHANTMENT =
            COMPONENTS.registerComponentType("blueprint_enchantment", b -> b
                    .persistent(Enchantment.CODEC)
                    .networkSynchronized(Enchantment.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WRATH_HITS =
            COMPONENTS.registerComponentType("wrath_hits", b -> b
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Building id and radius on a marker item; the block entity takes it over when the block is placed. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BuildingMarker>> BUILDING_MARKER =
            COMPONENTS.registerComponentType("building_marker", b -> b
                    .persistent(BuildingMarker.CODEC)
                    .networkSynchronized(BuildingMarker.STREAM_CODEC));

    private ModDataComponents() {}
}
