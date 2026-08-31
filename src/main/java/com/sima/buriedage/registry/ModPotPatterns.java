package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

/**
 * Sprite patterns for our four sherds. Registered so the textures are stitched onto the decorated
 * pot atlas; see ROADMAP for the one vanilla hook that is still missing to show them on a pot.
 */
public final class ModPotPatterns {
    public static final DeferredRegister<DecoratedPotPattern> PATTERNS =
            DeferredRegister.create(Registries.DECORATED_POT_PATTERN, TheBuriedAge.MODID);

    public static final DeferredHolder<DecoratedPotPattern, DecoratedPotPattern> LIGHTNING = register("lightning");
    public static final DeferredHolder<DecoratedPotPattern, DecoratedPotPattern> LYRE = register("lyre");
    public static final DeferredHolder<DecoratedPotPattern, DecoratedPotPattern> HOPLITE = register("hoplite");
    public static final DeferredHolder<DecoratedPotPattern, DecoratedPotPattern> AMPHORA = register("amphora");

    private static DeferredHolder<DecoratedPotPattern, DecoratedPotPattern> register(String name) {
        return PATTERNS.register(name, () -> new DecoratedPotPattern(
                Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, name + "_pottery_pattern")));
    }

    /** Which sherd shows which pattern. Vanilla keeps this in an immutable map we cannot extend,
     * so DecoratedPotPatternsMixin consults this one first. */
    public static @Nullable ResourceKey<DecoratedPotPattern> forItem(Item item) {
        if (item == ModItems.LIGHTNING_POTTERY_SHERD.get()) {
            return LIGHTNING.getKey();
        } else if (item == ModItems.LYRE_POTTERY_SHERD.get()) {
            return LYRE.getKey();
        } else if (item == ModItems.HOPLITE_POTTERY_SHERD.get()) {
            return HOPLITE.getKey();
        } else if (item == ModItems.AMPHORA_POTTERY_SHERD.get()) {
            return AMPHORA.getKey();
        }

        return null;
    }

    private ModPotPatterns() {}
}
