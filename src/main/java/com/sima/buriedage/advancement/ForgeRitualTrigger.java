package com.sima.buriedage.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Fires when the Hephaestus Forge actually completes a ritual, not merely when it is struck. */
public class ForgeRitualTrigger extends SimpleCriterionTrigger<ForgeRitualTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** @param forged the item as it looks after the upgrade */
    public void trigger(ServerPlayer player, ItemStack forged) {
        this.trigger(player, instance -> instance.matches(forged));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
                i -> i.group(
                                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item))
                        .apply(i, TriggerInstance::new));

        public boolean matches(ItemStack forged) {
            return this.item.isEmpty() || this.item.get().test(forged);
        }

        public static Criterion<TriggerInstance> any() {
            return com.sima.buriedage.registry.ModTriggers.FORGE_RITUAL.get()
                    .createCriterion(new TriggerInstance(Optional.empty(), Optional.empty()));
        }
    }
}
