package com.sima.buriedage.journal;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sima.buriedage.item.AncientBlueprintItem;
import com.sima.buriedage.registry.ModDataComponents;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public sealed interface JournalEntry permits JournalEntry.Find, JournalEntry.Building {
    Codec<JournalEntry> CODEC = Codec.STRING.partialDispatch("type",
            entry -> DataResult.success(entry.typeName()),
            name -> switch (name) {
                case "find" -> DataResult.success(Find.MAP_CODEC);
                case "building" -> DataResult.success(Building.MAP_CODEC);
                default -> DataResult.error(() -> "Unknown journal entry type '" + name + "', expected \"find\" or \"building\"");
            });

    String typeName();

    int order();

    record Find(Identifier item, Optional<ResourceKey<Enchantment>> blueprint, int order) implements JournalEntry {
        public static final MapCodec<Find> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                        Identifier.CODEC.fieldOf("item").forGetter(Find::item),
                        ResourceKey.codec(Registries.ENCHANTMENT).optionalFieldOf("blueprint").forGetter(Find::blueprint),
                        Codec.INT.optionalFieldOf("order", 100).forGetter(Find::order))
                .apply(i, Find::new));

        @Override
        public String typeName() {
            return "find";
        }

        public Identifier key() {
            return this.blueprint
                    .map(key -> Identifier.fromNamespaceAndPath(this.item.getNamespace(),
                            this.item.getPath() + "/" + key.identifier().getNamespace() + "/" + key.identifier().getPath()))
                    .orElse(this.item);
        }

        public String descriptionKey() {
            Identifier key = this.key();
            return "journal.buried_age.find." + key.getNamespace() + "." + key.getPath().replace('/', '.');
        }

        public String fallbackDescriptionKey() {
            return "journal.buried_age.find." + this.item.getNamespace() + "." + this.item.getPath().replace('/', '.');
        }

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty() || !BuiltInRegistries.ITEM.containsKey(this.item)
                    || !stack.is(BuiltInRegistries.ITEM.getValue(this.item))) {
                return false;
            }
            if (this.blueprint.isEmpty()) {
                return true;
            }
            Holder<Enchantment> carried = stack.get(ModDataComponents.BLUEPRINT_ENCHANTMENT.get());
            return carried != null && carried.is(this.blueprint.get());
        }

        public ItemStack icon(HolderLookup.Provider registries) {
            if (!BuiltInRegistries.ITEM.containsKey(this.item)) {
                return ItemStack.EMPTY;
            }
            Item item = BuiltInRegistries.ITEM.getValue(this.item);
            if (this.blueprint.isPresent()) {
                Optional<Holder.Reference<Enchantment>> holder = registries.lookupOrThrow(Registries.ENCHANTMENT).get(this.blueprint.get());
                if (holder.isPresent()) {
                    return AncientBlueprintItem.create(holder.get());
                }
            }
            return new ItemStack(item);
        }
    }

    record Building(String id, Identifier icon, List<Identifier> loot, int order) implements JournalEntry {
        public static final MapCodec<Building> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                        Codec.STRING.fieldOf("id").forGetter(Building::id),
                        Identifier.CODEC.fieldOf("icon").forGetter(Building::icon),
                        Identifier.CODEC.listOf().optionalFieldOf("loot", List.of()).forGetter(Building::loot),
                        Codec.INT.optionalFieldOf("order", 100).forGetter(Building::order))
                .apply(i, Building::new));

        @Override
        public String typeName() {
            return "building";
        }

        public String group() {
            return BuildingMarker.groupOf(this.id);
        }

        private String langBase() {
            return "journal.buried_age.building." + this.id.replace('/', '.');
        }

        public String nameKey() {
            return this.langBase() + ".name";
        }

        public String descriptionKey() {
            return this.langBase() + ".desc";
        }

        public static String groupNameKey(String group) {
            return "journal.buried_age.city." + group;
        }
    }
}
