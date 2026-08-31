package com.sima.buriedage.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.sima.buriedage.TheBuriedAge;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Datagen covers registry data only: the enchantment, its tags and the block loot table. Models,
 * blockstates and language files are hand-authored under src/main/resources, because those are the
 * files the artist and the translator replace directly.
 */
@EventBusSubscriber(modid = TheBuriedAge.MODID)
public final class ModDataGenerators {
    private static final RegistrySetBuilder DATAPACK_ENTRIES = new RegistrySetBuilder()
            .add(Registries.ENCHANTMENT, ModEnchantmentGenerator::bootstrap);

    private ModDataGenerators() {}

    @SubscribeEvent
    static void onGatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();

        DatapackBuiltinEntriesProvider datapackEntries = generator.addProvider(true, new DatapackBuiltinEntriesProvider(
                output, registries, DATAPACK_ENTRIES, Set.of(TheBuriedAge.MODID)));
        // Tags are written against the patched lookup so the new enchantment resolves.
        generator.addProvider(true, new ModEnchantmentTagsProvider(output, datapackEntries.getRegistryProvider()));
        generator.addProvider(true, new ModItemTagsProvider(output, registries));
        // Vanilla lookup on purpose: the patched one re-decodes every enchantment, and item tags do
        // not exist during datagen. Advancements that name our own enchantment are hand-written.
        generator.addProvider(true, new net.minecraft.data.advancements.AdvancementProvider(
                output, registries, List.of(new ModAdvancementProvider())));
        generator.addProvider(true, new LootTableProvider(
                output,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)),
                registries));
    }
}
