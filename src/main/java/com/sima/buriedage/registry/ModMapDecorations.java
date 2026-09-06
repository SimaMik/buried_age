package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMapDecorations {
    public static final DeferredRegister<MapDecorationType> MAP_DECORATIONS =
            DeferredRegister.create(Registries.MAP_DECORATION_TYPE, TheBuriedAge.MODID);

    /**
     * The marker on the wandering trader's map: sprite {@code textures/map/decorations/buried_temple.png}.
     * An exploration-map element, like the trial chamber icon, so the map centres on the city and
     * the icon stays put as the player wanders.
     */
    public static final DeferredHolder<MapDecorationType, MapDecorationType> BURIED_TEMPLE =
            MAP_DECORATIONS.register("buried_temple", () -> new MapDecorationType(
                    Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "buried_temple"), true, 0xC9C3B0, true, false));

    private ModMapDecorations() {}
}
