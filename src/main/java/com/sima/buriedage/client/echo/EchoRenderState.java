package com.sima.buriedage.client.echo;

import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Extends the vanilla villager state because VillagerModel is typed to it. Adds the two things
 * this mod cares about: which clothes to draw, and how faded the ghost currently is.
 */
public class EchoRenderState extends VillagerRenderState {
    public @Nullable Identifier professionTexture;
    public float opacity = 1.0F;
}
