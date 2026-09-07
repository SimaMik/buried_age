package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.entity.EchoEntity;
import com.sima.buriedage.entity.PegasusEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheBuriedAge.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EchoEntity>> ECHO =
            ENTITIES.registerEntityType("echo", EchoEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10).noSave().fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<PegasusEntity>> PEGASUS =
            ENTITIES.registerEntityType("pegasus", PegasusEntity::new, MobCategory.CREATURE,
                    builder -> builder.sized(1.3964844F, 1.9F).eyeHeight(1.6F).passengerAttachments(1.34375F).clientTrackingRange(10));

    private ModEntities() {}
}
