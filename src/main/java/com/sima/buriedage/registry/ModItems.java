package com.sima.buriedage.registry;

import java.util.List;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.item.AncientBlueprintItem;
import com.sima.buriedage.item.BuildingMarkerItem;
import com.sima.buriedage.item.FieldJournalItem;
import com.sima.buriedage.item.HephaestusHammerItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheBuriedAge.MODID);

    public static final DeferredItem<AncientBlueprintItem> ANCIENT_BLUEPRINT =
            ITEMS.registerItem("ancient_blueprint", AncientBlueprintItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<HephaestusHammerItem> HEPHAESTUS_HAMMER =
            ITEMS.registerItem("hephaestus_hammer", HephaestusHammerItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<BlockItem> HEPHAESTUS_FORGE =
            ITEMS.registerSimpleBlockItem(ModBlocks.HEPHAESTUS_FORGE);

    public static final DeferredItem<BuildingMarkerItem> BUILDING_MARKER =
            ITEMS.registerItem("building_marker", p -> new BuildingMarkerItem(ModBlocks.BUILDING_MARKER.get(), p));

    public static final DeferredItem<FieldJournalItem> FIELD_JOURNAL =
            ITEMS.registerItem("field_journal", FieldJournalItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<BlockItem> PEGASUS_EGG =
            ITEMS.registerSimpleBlockItem(ModBlocks.PEGASUS_EGG);

    public static final DeferredItem<BlockItem> CAVITY_MARKER =
            ITEMS.registerSimpleBlockItem(ModBlocks.CAVITY_MARKER);

    public static final DeferredItem<Item> LIGHTNING_POTTERY_SHERD = ITEMS.registerSimpleItem("lightning_pottery_sherd");
    public static final DeferredItem<Item> LYRE_POTTERY_SHERD = ITEMS.registerSimpleItem("lyre_pottery_sherd");
    public static final DeferredItem<Item> HOPLITE_POTTERY_SHERD = ITEMS.registerSimpleItem("hoplite_pottery_sherd");
    public static final DeferredItem<Item> AMPHORA_POTTERY_SHERD = ITEMS.registerSimpleItem("amphora_pottery_sherd");

    public static final List<DeferredItem<Item>> SHERDS =
            List.of(LIGHTNING_POTTERY_SHERD, LYRE_POTTERY_SHERD, HOPLITE_POTTERY_SHERD, AMPHORA_POTTERY_SHERD);

    private ModItems() {}
}
