package com.sima.buriedage.client.pegasus;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class PegasusClientBridge {
    private PegasusClientBridge() {}

    public static Input input(Player rider) {
        return rider instanceof LocalPlayer local ? local.input.keyPresses : Input.EMPTY;
    }

    public static void send(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
