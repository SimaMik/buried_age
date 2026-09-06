package com.sima.buriedage.event;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.entity.PegasusEntity;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A rider cannot aim at the mount under them, so a golden apple used from the saddle goes to the
 * pegasus when the pegasus has a use for it: an empty bar, missing health, a foal to grow, or a
 * mate to court with the enchanted one. Otherwise the rider eats it as usual. Decided from synced
 * values only, so client and server agree and the client never starts eating what the server feeds.
 */
@EventBusSubscriber(modid = TheBuriedAge.MODID)
public final class PegasusFeeding {
    private PegasusFeeding() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!(player.getVehicle() instanceof PegasusEntity pegasus) || !pegasus.isTamed()
                || !(stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE))) {
            return;
        }

        boolean enchanted = stack.is(Items.ENCHANTED_GOLDEN_APPLE);
        boolean wants = pegasus.getStamina() < 1.0F || pegasus.getHealth() < pegasus.getMaxHealth()
                || pegasus.isBaby() || (enchanted && !pegasus.isBaby());
        if (!wants) {
            return;
        }

        InteractionResult result = pegasus.fedFood(player, stack);
        event.setCancellationResult(result.consumesAction() ? result : InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
