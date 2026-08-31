package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {
    public static final ResourceKey<Enchantment> WRATH_OF_ZEUS = ResourceKey.create(
            Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "wrath_of_zeus"));

    /**
     * Which enchantments an Ancient Blueprint can be found for. Data-driven on purpose: the pool is
     * changed by editing the tag, not the code.
     */
    public static final TagKey<Enchantment> BLUEPRINT_POOL = TagKey.create(
            Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "blueprint_pool"));

    private ModEnchantments() {}
}
