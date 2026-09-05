package com.sima.buriedage.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/** One trigger for the pegasus milestones; {@code event} is "hatched", "first_flight" or "icarus". */
public class PegasusTrigger extends SimpleCriterionTrigger<PegasusTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String event) {
        this.trigger(player, instance -> instance.matches(event));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String event)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
                i -> i.group(
                                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                                Codec.STRING.fieldOf("event").forGetter(TriggerInstance::event))
                        .apply(i, TriggerInstance::new));

        public boolean matches(String fired) {
            return this.event.equals(fired);
        }
    }
}
