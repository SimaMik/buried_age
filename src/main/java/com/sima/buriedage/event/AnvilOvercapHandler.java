package com.sima.buriedage.event;

import com.sima.buriedage.TheBuriedAge;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * The vanilla anvil clamps any enchantment it touches back to its own max level, so combining a
 * Sharpness VI sword with a Sharpness book would quietly undo the forge. This puts the higher level
 * back into the anvil output; every other anvil behaviour is left alone.
 */
@EventBusSubscriber(modid = TheBuriedAge.MODID)
public final class AnvilOvercapHandler {
    private AnvilOvercapHandler() {}

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack output = event.getOutput();
        if (output.isEmpty()) {
            return;
        }

        ItemStack input = event.getLeft();
        ItemEnchantments before = EnchantmentHelper.getEnchantmentsForCrafting(input);
        if (before.isEmpty()) {
            return;
        }

        DataComponentType<ItemEnchantments> componentType = EnchantmentHelper.getComponentType(output);
        ItemEnchantments after = output.getOrDefault(componentType, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable restored = new ItemEnchantments.Mutable(after);
        boolean changed = false;

        for (Holder<Enchantment> enchantment : before.keySet()) {
            int oldLevel = before.getLevel(enchantment);
            if (oldLevel > enchantment.value().getMaxLevel() && after.getLevel(enchantment) < oldLevel) {
                restored.set(enchantment, oldLevel);
                changed = true;
            }
        }

        if (changed) {
            ItemStack fixed = output.copy();
            fixed.set(componentType, restored.toImmutable());
            event.setOutput(fixed);
        }
    }
}
