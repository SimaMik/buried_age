package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    /**
     * What Wrath of Zeus may be forged onto. Vanilla has no tag for "melee minus axes", so this is
     * swords and spears plus the mace.
     */
    public static final TagKey<Item> WRATH_OF_ZEUS_TARGETS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "enchantable/wrath_of_zeus"));

    /** Our four sherds, for loot tables and the "collect them all" advancement. */
    public static final TagKey<Item> BURIED_AGE_SHERDS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "pottery_sherds"));

    private ModTags() {}
}
