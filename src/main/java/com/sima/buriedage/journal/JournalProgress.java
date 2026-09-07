package com.sima.buriedage.journal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record JournalProgress(Set<Identifier> finds, Set<String> buildings) {
    public static final JournalProgress EMPTY = new JournalProgress(Set.of(), Set.of());

    public static final MapCodec<JournalProgress> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Identifier.CODEC.listOf().optionalFieldOf("finds", List.of()).forGetter(p -> new ArrayList<>(p.finds)),
                    Codec.STRING.listOf().optionalFieldOf("buildings", List.of()).forGetter(p -> new ArrayList<>(p.buildings)))
            .apply(i, JournalProgress::fromLists));

    public static final StreamCodec<RegistryFriendlyByteBuf, JournalProgress> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), p -> new ArrayList<>(p.finds),
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), p -> new ArrayList<>(p.buildings),
            JournalProgress::fromLists);

    private static JournalProgress fromLists(List<Identifier> finds, List<String> buildings) {
        return new JournalProgress(
                Collections.unmodifiableSet(new LinkedHashSet<>(finds)),
                Collections.unmodifiableSet(new LinkedHashSet<>(buildings)));
    }

    public boolean hasFind(Identifier key) {
        return this.finds.contains(key);
    }

    public boolean hasBuilding(String id) {
        return this.buildings.contains(id);
    }

    public JournalProgress withFind(Identifier key) {
        Set<Identifier> next = new LinkedHashSet<>(this.finds);
        next.add(key);
        return new JournalProgress(Collections.unmodifiableSet(next), this.buildings);
    }

    public JournalProgress withBuilding(String id) {
        Set<String> next = new LinkedHashSet<>(this.buildings);
        next.add(id);
        return new JournalProgress(this.finds, Collections.unmodifiableSet(next));
    }
}
