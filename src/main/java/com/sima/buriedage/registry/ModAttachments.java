package com.sima.buriedage.registry;

import java.util.function.Supplier;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.journal.JournalProgress;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TheBuriedAge.MODID);

    /**
     * Journal progress lives on the player, not on the book item: it survives death and a lost book,
     * and only the owning player ever receives it.
     */
    public static final Supplier<AttachmentType<JournalProgress>> JOURNAL = ATTACHMENTS.register("journal",
            () -> AttachmentType.builder(() -> JournalProgress.EMPTY)
                    .serialize(JournalProgress.MAP_CODEC)
                    .copyOnDeath()
                    .sync((holder, player) -> holder == player, JournalProgress.STREAM_CODEC)
                    .build());

    private ModAttachments() {}
}
