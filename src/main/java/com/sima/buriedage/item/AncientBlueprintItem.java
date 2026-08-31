package com.sima.buriedage.item;

import java.util.function.Consumer;

import com.sima.buriedage.registry.ModDataComponents;
import com.sima.buriedage.registry.ModEnchantments;
import com.sima.buriedage.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

public class AncientBlueprintItem extends Item {
    public AncientBlueprintItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemStack create(Holder<Enchantment> enchantment) {
        ItemStack stack = new ItemStack(ModItems.ANCIENT_BLUEPRINT.get());
        stack.set(ModDataComponents.BLUEPRINT_ENCHANTMENT.get(), enchantment);
        return stack;
    }

    public static @Nullable Holder<Enchantment> getEnchantment(ItemStack stack) {
        return stack.get(ModDataComponents.BLUEPRINT_ENCHANTMENT.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        Holder<Enchantment> enchantment = getEnchantment(stack);
        if (enchantment == null) {
            return super.getName(stack);
        }

        return Component.translatable("item.buried_age.ancient_blueprint.named", enchantment.value().description());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        Holder<Enchantment> enchantment = getEnchantment(stack);
        if (enchantment != null) {
            String effectKey = enchantment.is(ModEnchantments.WRATH_OF_ZEUS)
                    ? "item.buried_age.ancient_blueprint.tooltip.wrath"
                    : "item.buried_age.ancient_blueprint.tooltip.overcap";
            builder.accept(Component.translatable(effectKey).withStyle(ChatFormatting.GRAY));
        }

        builder.accept(Component.translatable("item.buried_age.ancient_blueprint.tooltip.use")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
