package com.sima.buriedage.network;

import java.util.List;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.journal.JournalBook;
import com.sima.buriedage.journal.JournalEntry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The whole journal entry list, sent to a player on login and after every datapack reload. */
public record JournalBookPayload(List<JournalEntry> entries) implements CustomPacketPayload {
    public static final Type<JournalBookPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "journal_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JournalBookPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(JournalEntry.CODEC.listOf())
                    .map(JournalBookPayload::new, JournalBookPayload::entries);

    public static JournalBookPayload of(JournalBook book) {
        return new JournalBookPayload(book.all());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
