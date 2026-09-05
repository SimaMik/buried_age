package com.sima.buriedage.journal;

import java.util.HashMap;
import java.util.Map;

import com.sima.buriedage.TheBuriedAge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads every {@code data/<namespace>/journal/**.json} file into the server-side book. Adding a
 * building or a find is a new file in that folder, nothing else.
 */
public final class JournalEntries extends SimpleJsonResourceReloadListener<JournalEntry> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "journal");

    private static volatile JournalBook server = JournalBook.EMPTY;

    public JournalEntries() {
        super(JournalEntry.CODEC, FileToIdConverter.json("journal"));
    }

    @Override
    protected void apply(Map<Identifier, JournalEntry> entries, ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JournalEntry> valid = new HashMap<>(entries);
        valid.entrySet().removeIf(entry -> {
            if (entry.getValue() instanceof JournalEntry.Find find && !BuiltInRegistries.ITEM.containsKey(find.item())) {
                TheBuriedAge.LOGGER.warn("Journal entry {} names an unknown item {} and was skipped", entry.getKey(), find.item());
                return true;
            }
            return false;
        });
        server = new JournalBook(valid);
        TheBuriedAge.LOGGER.info("Loaded {} journal entries ({} finds, {} city tabs)",
                valid.size(), server.finds().size(), server.groups().size());
    }

    /** The book as the server currently knows it. */
    public static JournalBook server() {
        return server;
    }
}
