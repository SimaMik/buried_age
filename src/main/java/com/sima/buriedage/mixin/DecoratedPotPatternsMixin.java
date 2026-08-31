package com.sima.buriedage.mixin;

import com.sima.buriedage.registry.ModPotPatterns;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotPatterns.class)
public class DecoratedPotPatternsMixin {
    @Inject(method = "getPatternFromItem", at = @At("HEAD"), cancellable = true)
    private static void buriedAge$ourSherds(Item item, CallbackInfoReturnable<ResourceKey<DecoratedPotPattern>> cir) {
        ResourceKey<DecoratedPotPattern> ours = ModPotPatterns.forItem(item);
        if (ours != null) {
            cir.setReturnValue(ours);
        }
    }
}
