package com.sima.buriedage.journal;

import com.sima.buriedage.registry.ModAttachments;
import com.sima.buriedage.registry.ModTriggers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** The only two ways progress ever changes. Server side; the attachment carries the result to the client. */
public final class JournalService {
    private JournalService() {}

    public static JournalProgress progress(ServerPlayer player) {
        return player.getData(ModAttachments.JOURNAL);
    }

    /** Records a find the first time it is seen; later calls are free. */
    public static void discoverFind(ServerPlayer player, JournalEntry.Find find, ItemStack seen) {
        Identifier key = find.key();
        JournalProgress current = progress(player);
        if (current.hasFind(key)) {
            return;
        }

        player.setData(ModAttachments.JOURNAL, current.withFind(key));
        celebrate(player, Component.translatable("message.buried_age.journal.find", seen.getHoverName()));
    }

    /**
     * Called every second for every player standing inside a marker. The advancement trigger fires
     * each time so a criterion added later still completes; the journal and the message only once.
     */
    public static void visitBuilding(ServerPlayer player, String building) {
        ModTriggers.BUILDING_DISCOVERED.get().trigger(player, building);

        JournalProgress current = progress(player);
        if (current.hasBuilding(building)) {
            return;
        }

        player.setData(ModAttachments.JOURNAL, current.withBuilding(building));
        JournalEntry.Building entry = JournalEntries.server().building(building);
        if (entry != null) {
            celebrate(player, Component.translatable("message.buried_age.journal.building",
                    Component.translatable(entry.nameKey())));
        }
    }

    private static void celebrate(ServerPlayer player, Component message) {
        player.sendSystemMessage(message, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    static boolean isKnownItem(Identifier id) {
        return BuiltInRegistries.ITEM.containsKey(id);
    }
}
