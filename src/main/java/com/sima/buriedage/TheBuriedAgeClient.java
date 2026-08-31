package com.sima.buriedage;

import com.sima.buriedage.client.HephaestusForgeRenderer;
import com.sima.buriedage.client.echo.EchoRenderer;
import com.sima.buriedage.registry.ModBlockEntities;
import com.sima.buriedage.registry.ModEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = TheBuriedAge.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TheBuriedAge.MODID, value = Dist.CLIENT)
public class TheBuriedAgeClient {
    public TheBuriedAgeClient(ModContainer container) {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.HEPHAESTUS_FORGE.get(), HephaestusForgeRenderer::new);
        event.registerEntityRenderer(ModEntities.ECHO.get(), EchoRenderer::new);
    }
}
