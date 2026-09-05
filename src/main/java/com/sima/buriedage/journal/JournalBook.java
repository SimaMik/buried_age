package com.sima.buriedage.journal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Every loaded entry, sorted and indexed. Built once per datapack load and shipped to clients whole. */
public final class JournalBook {
    public static final JournalBook EMPTY = new JournalBook(Map.of());

    private final List<JournalEntry.Find> finds;
    private final Map<String, List<JournalEntry.Building>> buildingsByGroup;
    private final Map<Item, List<JournalEntry.Find>> findsByItem;
    private final Map<String, JournalEntry.Building> buildingsById;
    private final List<JournalEntry> all;

    public JournalBook(Map<Identifier, JournalEntry> entries) {
        List<Identifier> ids = new ArrayList<>(entries.keySet());
        ids.sort(Comparator.<Identifier>comparingInt(id -> entries.get(id).order()).thenComparing(Identifier::toString));

        List<JournalEntry.Find> finds = new ArrayList<>();
        Map<String, List<JournalEntry.Building>> groups = new LinkedHashMap<>();
        Map<Item, List<JournalEntry.Find>> byItem = new HashMap<>();
        Map<String, JournalEntry.Building> byId = new HashMap<>();
        List<JournalEntry> all = new ArrayList<>();
        for (Identifier id : ids) {
            JournalEntry entry = entries.get(id);
            all.add(entry);
            switch (entry) {
                case JournalEntry.Find find -> {
                    finds.add(find);
                    if (BuiltInRegistries.ITEM.containsKey(find.item())) {
                        byItem.computeIfAbsent(BuiltInRegistries.ITEM.getValue(find.item()), k -> new ArrayList<>()).add(find);
                    }
                }
                case JournalEntry.Building building -> {
                    groups.computeIfAbsent(building.group(), k -> new ArrayList<>()).add(building);
                    byId.put(building.id(), building);
                }
            }
        }

        this.finds = List.copyOf(finds);
        this.buildingsByGroup = Map.copyOf(groups);
        this.findsByItem = Map.copyOf(byItem);
        this.buildingsById = Map.copyOf(byId);
        this.all = List.copyOf(all);
        this.groupOrder = List.copyOf(groups.keySet());
    }

    private final List<String> groupOrder;

    /** Rebuilds a book from the flat list a client received. Ids are synthetic; order is already final. */
    public static JournalBook of(List<JournalEntry> entries) {
        Map<Identifier, JournalEntry> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            map.put(Identifier.fromNamespaceAndPath("buried_age", "synced/" + String.format("%05d", i)), entries.get(i));
        }
        return new JournalBook(map);
    }

    public List<JournalEntry> all() {
        return this.all;
    }

    public List<JournalEntry.Find> finds() {
        return this.finds;
    }

    /** Group keys in display order: the order of the first building of each group. */
    public List<String> groups() {
        return this.groupOrder;
    }

    public List<JournalEntry.Building> buildings(String group) {
        return this.buildingsByGroup.getOrDefault(group, List.of());
    }

    public List<JournalEntry.Find> findsFor(Item item) {
        return this.findsByItem.getOrDefault(item, List.of());
    }

    public JournalEntry.Building building(String id) {
        return this.buildingsById.get(id);
    }

    public boolean isEmpty() {
        return this.all.isEmpty();
    }
}
