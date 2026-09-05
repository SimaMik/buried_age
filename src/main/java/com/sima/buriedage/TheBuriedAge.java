package com.sima.buriedage;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.sima.buriedage.registry.ModAttachments;
import com.sima.buriedage.registry.ModBlockEntities;
import com.sima.buriedage.registry.ModBlocks;
import com.sima.buriedage.registry.ModCreativeTabs;
import com.sima.buriedage.registry.ModDataComponents;
import com.sima.buriedage.registry.ModEntities;
import com.sima.buriedage.registry.ModItems;
import com.sima.buriedage.registry.ModLootFunctions;
import com.sima.buriedage.registry.ModPotPatterns;
import com.sima.buriedage.registry.ModStructureTypes;
import com.sima.buriedage.registry.ModTriggers;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(TheBuriedAge.MODID)
public class TheBuriedAge {
    public static final String MODID = "buried_age";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheBuriedAge(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModPotPatterns.PATTERNS.register(modEventBus);
        ModLootFunctions.LOOT_FUNCTIONS.register(modEventBus);
        ModTriggers.TRIGGERS.register(modEventBus);
        ModStructureTypes.STRUCTURE_TYPES.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
    }
}
