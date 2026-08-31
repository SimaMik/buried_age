package com.sima.buriedage.data;

import com.sima.buriedage.registry.ModEnchantments;
import com.sima.buriedage.registry.ModTags;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantmentGenerator {
    private ModEnchantmentGenerator() {}

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);

        // Reachable only through an Ancient Blueprint: it is deliberately left out of
        // in_enchanting_table, on_random_loot and tradeable, which is what gates every vanilla source.
        context.register(ModEnchantments.WRATH_OF_ZEUS, Enchantment.enchantment(
                Enchantment.definition(
                        items.getOrThrow(ModTags.WRATH_OF_ZEUS_TARGETS),
                        1,
                        1,
                        Enchantment.constantCost(25),
                        Enchantment.constantCost(75),
                        4,
                        EquipmentSlotGroup.MAINHAND))
                .build(ModEnchantments.WRATH_OF_ZEUS.identifier()));
    }
}
