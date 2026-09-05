package com.sima.buriedage.network;

import com.sima.buriedage.TheBuriedAge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The rider's client, which simulates the flight, tells the server what just happened to the mount.
 * The server trusts only the kind of event and the speed; it checks that the sender is the rider.
 */
public record PegasusFlightPayload(int entityId, Kind kind, float speed) implements CustomPacketPayload {
    public static final Type<PegasusFlightPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "pegasus_flight"));

    public enum Kind {
        TAKEOFF, LANDING, CRASH, STALL;

        static final Kind[] VALUES = values();
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PegasusFlightPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PegasusFlightPayload::entityId,
            ByteBufCodecs.VAR_INT.map(i -> Kind.VALUES[Math.floorMod(i, Kind.VALUES.length)], Kind::ordinal), PegasusFlightPayload::kind,
            ByteBufCodecs.FLOAT, PegasusFlightPayload::speed,
            PegasusFlightPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
