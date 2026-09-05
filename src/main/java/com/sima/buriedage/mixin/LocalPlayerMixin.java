package com.sima.buriedage.mixin;

import com.sima.buriedage.entity.PegasusEntity;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The horse jump bar shows the pegasus' stamina. Vanilla fills the bar from its own jump charge;
 * while riding a pegasus the value comes from the mount instead, and the bar reads the same sprites.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "getJumpRidingScale", at = @At("HEAD"), cancellable = true)
    private void buriedAge$pegasusStamina(CallbackInfoReturnable<Float> cir) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.getControlledVehicle() instanceof PegasusEntity pegasus) {
            cir.setReturnValue(pegasus.getStamina());
        }
    }
}
