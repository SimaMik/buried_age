package com.sima.buriedage.data;

import java.util.List;

import com.sima.buriedage.registry.ModEnchantments;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public final class BlueprintPool {
    public static final List<ResourceKey<Enchantment>> ENTRIES = List.of(
            Enchantments.SHARPNESS,
            Enchantments.SMITE,
            Enchantments.BANE_OF_ARTHROPODS,
            Enchantments.LOOTING,
            Enchantments.FIRE_ASPECT,
            Enchantments.KNOCKBACK,
            Enchantments.SWEEPING_EDGE,
            Enchantments.EFFICIENCY,
            Enchantments.FORTUNE,
            Enchantments.UNBREAKING,
            Enchantments.PROTECTION,
            Enchantments.FIRE_PROTECTION,
            Enchantments.BLAST_PROTECTION,
            Enchantments.PROJECTILE_PROTECTION,
            Enchantments.THORNS,
            Enchantments.RESPIRATION,
            Enchantments.FEATHER_FALLING,
            Enchantments.POWER,
            Enchantments.PUNCH,
            Enchantments.PIERCING,
            Enchantments.LOYALTY,
            Enchantments.IMPALING,
            Enchantments.RIPTIDE,
            Enchantments.DENSITY,
            Enchantments.WIND_BURST,
            Enchantments.LUNGE,
            Enchantments.LUCK_OF_THE_SEA,
            ModEnchantments.WRATH_OF_ZEUS);

    private BlueprintPool() {}
}
