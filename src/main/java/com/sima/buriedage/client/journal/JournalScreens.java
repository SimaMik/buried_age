package com.sima.buriedage.client.journal;

import net.minecraft.client.Minecraft;

public final class JournalScreens {
    private JournalScreens() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new JournalScreen());
    }
}
