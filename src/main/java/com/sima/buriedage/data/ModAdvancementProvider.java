package com.sima.buriedage.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.advancement.ForgeRitualTrigger;
import com.sima.buriedage.registry.ModDataComponents;
import com.sima.buriedage.registry.ModEnchantments;
import com.sima.buriedage.registry.ModItems;
import com.sima.buriedage.registry.ModTags;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.advancements.criterion.LootTableTrigger;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootTable;

/** The twelve advancements of the archaeologist tab. */
public class ModAdvancementProvider implements AdvancementSubProvider {
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, path);
    }

    private static ResourceKey<LootTable> lootTable(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, id(path));
    }

    private static Component title(String key) {
        return Component.translatable("advancements.buried_age." + key + ".title");
    }

    private static Component description(String key) {
        return Component.translatable("advancements.buried_age." + key + ".description");
    }

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        HolderLookup.RegistryLookup<net.minecraft.world.item.Item> items = registries.lookupOrThrow(Registries.ITEM);

        // 1 — root. Hand-written in src/main/resources, because datagen registries do not contain our
        // structure (it is a plain datapack file), so the location predicate cannot be built here.
        AdvancementHolder root = AdvancementSubProvider.createPlaceholder(id("root").toString());

        // 2 — brushing anything the city buried.
        AdvancementHolder dig = Advancement.Builder.advancement()
                .parent(root)
                .display(Items.BRUSH, title("dig"), description("dig"), null,
                        AdvancementType.TASK, true, true, false)
                .requirements(AdvancementRequirements.Strategy.OR)
                .addCriterion("city_gravel", LootTableTrigger.TriggerInstance.lootTableUsed(lootTable("archaeology/city_gravel")))
                .addCriterion("temple_gravel", LootTableTrigger.TriggerInstance.lootTableUsed(lootTable("archaeology/temple_gravel")))
                .save(output, id("dig").toString());

        // Collection branch.
        AdvancementHolder shards = Advancement.Builder.advancement()
                .parent(dig)
                .display(ModItems.HOPLITE_POTTERY_SHERD.get(), title("shards"), description("shards"), null,
                        AdvancementType.TASK, true, true, false)
                .addCriterion("all_four", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ModItems.SHERDS.stream().map(sherd -> ItemPredicate.Builder.item().of(items, sherd.get()))
                                .toArray(ItemPredicate.Builder[]::new)))
                .save(output, id("shards").toString());

        Advancement.Builder.advancement()
                .parent(shards)
                .display(Items.DECORATED_POT, title("potter"), description("potter"), null,
                        AdvancementType.GOAL, true, true, false)
                .addCriterion("crafted_pot", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(items, Items.DECORATED_POT)))
                .save(output, id("potter").toString());

        // Temple branch.
        AdvancementHolder cella = Advancement.Builder.advancement()
                .parent(dig)
                .display(Items.CHISELED_STONE_BRICKS, title("cella"), description("cella"), null,
                        AdvancementType.TASK, true, true, false)
                .addCriterion("in_cella", PlayerTrigger.TriggerInstance.located(
                        EntityPredicate.Builder.entity().located(LocationPredicate.Builder.location()
                                .setBlock(net.minecraft.advancements.criterion.BlockPredicate.Builder.block()
                                        .of(registries.lookupOrThrow(Registries.BLOCK),
                                                com.sima.buriedage.registry.ModBlocks.CELLA_MARKER.get())))))
                .save(output, id("cella").toString());

        AdvancementHolder blueprint = Advancement.Builder.advancement()
                .parent(cella)
                .display(ModItems.ANCIENT_BLUEPRINT.get(), title("blueprint"), description("blueprint"), null,
                        AdvancementType.TASK, true, true, false)
                .addCriterion("has_blueprint", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(items, ModItems.ANCIENT_BLUEPRINT.get())))
                .save(output, id("blueprint").toString());

        AdvancementHolder legacy = Advancement.Builder.advancement()
                .parent(blueprint)
                .display(ModItems.HEPHAESTUS_FORGE.get(), title("legacy"), description("legacy"), null,
                        AdvancementType.GOAL, true, true, false)
                .addCriterion("forged", ForgeRitualTrigger.TriggerInstance.any())
                .save(output, id("legacy").toString());

        // 12 — anything past the vanilla ceiling, whichever enchantment it was.
        Advancement.Builder overcap = Advancement.Builder.advancement()
                .parent(legacy)
                .display(Items.ENCHANTED_BOOK, title("beyond"), description("beyond"), null,
                        AdvancementType.CHALLENGE, true, true, false)
                .requirements(AdvancementRequirements.Strategy.OR);
        List<String> overcapCriteria = new ArrayList<>();
        for (Holder<Enchantment> enchantment : poolOf(enchantments)) {
            if (enchantment.value().getMaxLevel() < 2) {
                continue;
            }

            int beyond = enchantment.value().getMaxLevel() + 1;
            String name = enchantment.getRegisteredName().replace(':', '_').replace('/', '_');
            overcapCriteria.add(name);
            overcap.addCriterion(name, InventoryChangeTrigger.TriggerInstance.hasItems(
                    ItemPredicate.Builder.item().withComponents(hasEnchantment(enchantment, beyond))));
        }

        overcap.save(output, id("beyond").toString());

        // 11 lives in src/main/resources: it needs a criterion for our own enchantment, which the
        // datagen registries cannot resolve. Generating it here would only ever cover 27 of 28.

        // 7 — hidden: the treasury chest.
        Advancement.Builder.advancement()
                .parent(root)
                .display(Items.GOLD_INGOT, title("tomb_raider"), description("tomb_raider"), null,
                        AdvancementType.GOAL, true, true, true)
                .addCriterion("opened", LootTableTrigger.TriggerInstance.lootTableUsed(lootTable("chests/treasury")))
                .save(output, id("tomb_raider").toString());

        // 10 — hidden: a coin pouch, caught at the tables that can hand one out.
        Advancement.Builder.advancement()
                .parent(root)
                .display(Items.BUNDLE, title("purse"), description("purse"), null,
                        AdvancementType.TASK, true, true, true)
                .requirements(AdvancementRequirements.Strategy.OR)
                .addCriterion("treasury", LootTableTrigger.TriggerInstance.lootTableUsed(lootTable("chests/treasury")))
                .addCriterion("house_medium", LootTableTrigger.TriggerInstance.lootTableUsed(lootTable("chests/house_medium")))
                .addCriterion("house_rich", LootTableTrigger.TriggerInstance.lootTableUsed(lootTable("chests/house_rich")))
                .save(output, id("purse").toString());
    }

    /** Item predicate: carries this enchantment at least this deep. */
    private static DataComponentMatchers hasEnchantment(Holder<Enchantment> enchantment, int minLevel) {
        return DataComponentMatchers.Builder.components()
                .partial(DataComponentPredicates.ENCHANTMENTS, EnchantmentsPredicate.enchantments(
                        List.of(new EnchantmentPredicate(enchantment, MinMaxBounds.Ints.atLeast(minLevel)))))
                .build();
    }

    /** Item predicate: an Ancient Blueprint for exactly this enchantment. */
    private static DataComponentMatchers carriesBlueprint(Holder<Enchantment> enchantment) {
        return DataComponentMatchers.Builder.components()
                .exact(DataComponentExactPredicate.builder()
                        .expect(ModDataComponents.BLUEPRINT_ENCHANTMENT.get(), enchantment)
                        .build())
                .build();
    }

    /** Vanilla half of the pool. Wrath of Zeus is ours, is absent from this lookup, and is covered
     * by the hand-written wrath advancement instead. */
    private static List<Holder<Enchantment>> poolOf(HolderLookup.RegistryLookup<Enchantment> enchantments) {
        List<Holder<Enchantment>> found = new ArrayList<>();
        for (ResourceKey<Enchantment> key : BlueprintPool.ENTRIES) {
            enchantments.get(key).ifPresent(found::add);
        }

        return found;
    }
}
