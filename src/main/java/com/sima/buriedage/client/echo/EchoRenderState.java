package com.sima.buriedage.client.echo;

import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class EchoRenderState extends VillagerRenderState {
    public @Nullable Identifier professionTexture;
    public float opacity = 1.0F;
}
