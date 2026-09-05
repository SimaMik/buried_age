package com.sima.buriedage.network;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.entity.PegasusEntity;
import com.sima.buriedage.journal.ClientJournal;
import com.sima.buriedage.journal.JournalBook;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TheBuriedAge.MODID)
public final class ModNetwork {
    private ModNetwork() {}

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(JournalBookPayload.TYPE, JournalBookPayload.STREAM_CODEC,
                (payload, context) -> ClientJournal.accept(JournalBook.of(payload.entries())));
        registrar.playToServer(PegasusFlightPayload.TYPE, PegasusFlightPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer rider
                    && rider.level().getEntity(payload.entityId()) instanceof PegasusEntity pegasus
                    && pegasus.hasPassenger(rider)) {
                pegasus.applyFlightEvent(payload.kind(), payload.speed(), rider);
            }
        });
    }
}
