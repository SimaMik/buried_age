package com.sima.buriedage.event;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.registry.ModDataComponents;
import com.sima.buriedage.registry.ModEnchantments;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Wrath of Zeus: every fourth melee hit calls down a bolt on the victim. The bolt is spawned
 * visual-only so it never sets the world on fire; the damage and the brief burn are applied by hand.
 */
@EventBusSubscriber(modid = TheBuriedAge.MODID)
public final class WrathOfZeusHandler {
    /** Hits needed before the bolt lands. */
    public static final int HITS_PER_BOLT = 4;
    /** Balance numbers: vanilla lightning_ deals 5, and burns for 8 seconds. */
    public static final float BOLT_DAMAGE = 5.0F;
    public static final float BOLT_BURN_SECONDS = 3.0F;

    private WrathOfZeusHandler() {}

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        // Melee only: for arrows and the like the direct entity is the projectile, not the attacker.
        if (event.getSource().getDirectEntity() != attacker) {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (victim instanceof ArmorStand || !victim.isAlive()) {
            return;
        }

        Holder<Enchantment> wrath = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.WRATH_OF_ZEUS)
                .orElse(null);
        if (wrath == null) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (EnchantmentHelper.getItemEnchantmentLevel(wrath, weapon) <= 0) {
            return;
        }

        int hits = weapon.getOrDefault(ModDataComponents.WRATH_HITS.get(), 0) + 1;
        if (hits < HITS_PER_BOLT) {
            weapon.set(ModDataComponents.WRATH_HITS.get(), hits);
            return;
        }

        weapon.set(ModDataComponents.WRATH_HITS.get(), 0);
        strike(level, victim);
    }

    private static void strike(ServerLevel level, LivingEntity victim) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            Vec3 pos = victim.position();
            bolt.snapTo(pos.x, pos.y, pos.z);
            // Visual only gates both the block fire and the built-in damage, so we do the rest here.
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        victim.igniteForSeconds(BOLT_BURN_SECONDS);
        victim.hurtServer(level, level.damageSources().lightningBolt(), BOLT_DAMAGE);
    }
}
