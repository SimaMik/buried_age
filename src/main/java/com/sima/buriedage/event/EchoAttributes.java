package com.sima.buriedage.event;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.entity.EchoEntity;
import com.sima.buriedage.registry.ModEntities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** LivingEntity needs an attribute map even though the echo never uses any of it. */
@EventBusSubscriber(modid = TheBuriedAge.MODID)
public final class EchoAttributes {
    private EchoAttributes() {}

    @SubscribeEvent
    public static void onCreateAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ECHO.get(), EchoEntity.createAttributes().build());
    }
}
