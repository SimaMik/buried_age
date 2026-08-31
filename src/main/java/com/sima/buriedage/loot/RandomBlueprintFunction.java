package com.sima.buriedage.loot;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sima.buriedage.registry.ModDataComponents;
import com.sima.buriedage.registry.ModEnchantments;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Stamps a random enchantment from a tag onto an Ancient Blueprint, so a single loot entry covers
 * the whole pool instead of one entry per enchantment.
 *
 * <pre>{ "function": "buried_age:random_blueprint" }</pre>
 * <p>Optionally takes {@code "pool"} to draw from a different enchantment tag.
 */
public class RandomBlueprintFunction extends LootItemConditionalFunction {
    public static final MapCodec<RandomBlueprintFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> commonFields(i)
                    .and(TagKey.hashedCodec(Registries.ENCHANTMENT)
                            .optionalFieldOf("pool", ModEnchantments.BLUEPRINT_POOL)
                            .forGetter(f -> f.pool))
                    .apply(i, RandomBlueprintFunction::new));

    private final TagKey<Enchantment> pool;

    private RandomBlueprintFunction(List<LootItemCondition> predicates, TagKey<Enchantment> pool) {
        super(predicates);
        this.pool = pool;
    }

    @Override
    public MapCodec<RandomBlueprintFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        HolderSet<Enchantment> candidates = context.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(this.pool)
                .orElse(null);
        if (candidates == null || candidates.size() == 0) {
            return itemStack;
        }

        Holder<Enchantment> picked = candidates.get(context.getRandom().nextInt(candidates.size()));
        itemStack.set(ModDataComponents.BLUEPRINT_ENCHANTMENT.get(), picked);
        return itemStack;
    }

    @Override
    public String toString() {
        return "random_blueprint[" + this.pool.location() + "]";
    }
}
