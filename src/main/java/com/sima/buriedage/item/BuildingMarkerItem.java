package com.sima.buriedage.item;

import java.util.function.Consumer;

import com.sima.buriedage.journal.BuildingMarker;
import com.sima.buriedage.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The marker as an item. The building id sits in its component and is shown on the tooltip. */
public class BuildingMarkerItem extends BlockItem {
    public BuildingMarkerItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public static BuildingMarker markerOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BUILDING_MARKER.get(), BuildingMarker.EMPTY);
    }

    @Override
    public Component getName(ItemStack stack) {
        BuildingMarker marker = markerOf(stack);
        return marker.isSet()
                ? Component.translatable("item.buried_age.building_marker.named", marker.building())
                : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> lines, TooltipFlag flag) {
        BuildingMarker marker = markerOf(stack);
        if (marker.isSet()) {
            lines.accept(Component.translatable("item.buried_age.building_marker.tooltip.radius", marker.radius())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            lines.accept(Component.translatable("item.buried_age.building_marker.tooltip.unset")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
