package com.sima.buriedage.data;

import java.util.concurrent.CompletableFuture;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.registry.ModEnchantments;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;

public class ModEnchantmentTagsProvider extends EnchantmentTagsProvider {
    public ModEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, TheBuriedAge.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        var pool = this.tag(ModEnchantments.BLUEPRINT_POOL);
        BlueprintPool.ENTRIES.forEach(pool::add);

        this.tag(EnchantmentTags.TOOLTIP_ORDER).add(ModEnchantments.WRATH_OF_ZEUS);
    }
}
