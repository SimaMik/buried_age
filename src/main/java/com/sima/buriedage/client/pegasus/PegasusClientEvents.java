package com.sima.buriedage.client.pegasus;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.entity.PegasusEntity;
import com.sima.buriedage.entity.PegasusTuning;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

@EventBusSubscriber(modid = TheBuriedAge.MODID, value = Dist.CLIENT)
public final class PegasusClientEvents {
    private PegasusClientEvents() {}

    /** The view widens with airspeed, the way the elytra does it. */
    @SubscribeEvent
    static void onComputeFov(ComputeFovModifierEvent event) {
        if (event.getPlayer().getVehicle() instanceof PegasusEntity pegasus && pegasus.isFlying()) {
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F + pegasus.getAirspeed() * PegasusTuning.FOV_PER_SPEED));
        }
    }
}
