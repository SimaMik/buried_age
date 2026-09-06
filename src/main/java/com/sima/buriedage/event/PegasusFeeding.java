package com.sima.buriedage.event;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.entity.PegasusEntity;
import com.sima.buriedage.entity.PegasusTuning;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A rider cannot aim at the mount under them, so a plain golden apple used from the saddle goes to
 * the pegasus when its bar is nearly empty; any other time, and the enchanted apple always, the
 * rider eats it as usual. Decided from the synced bar only, so client and server agree and the
 * client never starts eating what the server feeds.
 */
@EventBusSubscriber(modid = TheBuriedAge.MODID)
public final class PegasusFeeding {
    private PegasusFeeding() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!(player.getVehicle() instanceof PegasusEntity pegasus) || !pegasus.isTamed()) {
            return;
        }

        if (!stack.is(Items.GOLDEN_APPLE) || pegasus.getStamina() >= PegasusTuning.SADDLE_FEED_STAMINA) {
            return;
        }

        InteractionResult result = pegasus.fedFood(player, stack);
        event.setCancellationResult(result.consumesAction() ? result : InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
