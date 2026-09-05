package com.sima.buriedage.item;

import com.sima.buriedage.client.journal.JournalScreens;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A key to the journal screen and nothing more: all progress lives on the player. */
public class FieldJournalItem extends Item {
    public FieldJournalItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            JournalScreens.open();
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }
}
