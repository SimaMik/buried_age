package com.sima.buriedage.journal;

import java.util.List;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.network.JournalBookPayload;
import com.sima.buriedage.registry.ModAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Wires the journal into the game loop: loads the entries, ships them to clients, and looks through
 * every player's inventory once a second for items the journal is waiting on.
 */
@EventBusSubscriber(modid = TheBuriedAge.MODID)
public final class JournalEvents {
    /** Ticks between two inventory scans. One second is plenty; the scan is a few dozen map lookups. */
    private static final int SCAN_INTERVAL = 20;

    private JournalEvents() {}

    @SubscribeEvent
    static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(JournalEntries.ID, new JournalEntries());
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        JournalBookPayload payload = JournalBookPayload.of(JournalEntries.server());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.syncData(ModAttachments.JOURNAL.get());
        }
    }

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.syncData(ModAttachments.JOURNAL.get());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % SCAN_INTERVAL != 0) {
            return;
        }

        JournalBook book = JournalEntries.server();
        if (book.isEmpty() || player.isSpectator() || !JournalService.carriesJournal(player)) {
            return;
        }

        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            check(player, book, stack);
        }
        check(player, book, player.getOffhandItem());
    }

    private static void check(ServerPlayer player, JournalBook book, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        List<JournalEntry.Find> candidates = book.findsFor(stack.getItem());
        for (JournalEntry.Find find : candidates) {
            if (find.matches(stack)) {
                JournalService.discoverFind(player, find, stack);
            }
        }
    }
}
