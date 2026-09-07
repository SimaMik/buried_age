package com.sima.buriedage.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sima.buriedage.registry.ModTriggers;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class BuildingDiscoveredTrigger extends SimpleCriterionTrigger<BuildingDiscoveredTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String building) {
        this.trigger(player, instance -> instance.matches(building));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<String> building)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
                i -> i.group(
                                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                                Codec.STRING.optionalFieldOf("building").forGetter(TriggerInstance::building))
                        .apply(i, TriggerInstance::new));

        public boolean matches(String visited) {
            return this.building.isEmpty() || this.building.get().equals(visited);
        }

        public static Criterion<TriggerInstance> of(String building) {
            return ModTriggers.BUILDING_DISCOVERED.get()
                    .createCriterion(new TriggerInstance(Optional.empty(), Optional.of(building)));
        }
    }
}
