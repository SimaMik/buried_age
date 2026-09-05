package com.sima.buriedage.client.journal;

import net.minecraft.client.Minecraft;

/** The one client-only call the journal item makes. Kept apart so the item class never loads client classes on a server. */
public final class JournalScreens {
    private JournalScreens() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new JournalScreen());
    }
}
