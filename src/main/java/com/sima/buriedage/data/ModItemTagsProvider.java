package com.sima.buriedage.data;

import java.util.concurrent.CompletableFuture;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.registry.ModItems;
import com.sima.buriedage.registry.ModTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

public class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, TheBuriedAge.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(ModTags.WRATH_OF_ZEUS_TARGETS)
                .addTag(ItemTags.MELEE_WEAPON_ENCHANTABLE)
                .add(Items.MACE);

        ModItems.SHERDS.forEach(sherd -> this.tag(ModTags.BURIED_AGE_SHERDS).add(sherd.get()));
        ModItems.SHERDS.forEach(sherd -> this.tag(ItemTags.DECORATED_POT_SHERDS).add(sherd.get()));
    }
}
