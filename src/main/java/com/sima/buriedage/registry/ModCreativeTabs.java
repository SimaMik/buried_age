package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.item.AncientBlueprintItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheBuriedAge.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BURIED_AGE =
            CREATIVE_MODE_TABS.register("buried_age", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.buried_age"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.HEPHAESTUS_HAMMER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.HEPHAESTUS_FORGE.get());
                        output.accept(ModItems.HEPHAESTUS_HAMMER.get());
                        ModItems.SHERDS.forEach(sherd -> output.accept(sherd.get()));
                        output.accept(ModItems.CELLA_MARKER.get());
                        output.accept(ModItems.CAVITY_MARKER.get());
                        output.accept(ModItems.ECHO_SPAWN_EGG.get());
                        parameters.holders()
                                .lookupOrThrow(Registries.ENCHANTMENT)
                                .get(ModEnchantments.BLUEPRINT_POOL)
                                .ifPresent(pool -> pool.forEach(enchantment ->
                                        output.accept(AncientBlueprintItem.create(enchantment))));
                    })
                    .build());

    private ModCreativeTabs() {}
}
