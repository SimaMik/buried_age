package com.sima.buriedage.registry;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.worldgen.BuriedCityPiece;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructurePieceTypes {
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, TheBuriedAge.MODID);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> CITY_PIECE =
            STRUCTURE_PIECE_TYPES.register("city_piece", () -> BuriedCityPiece::new);

    private ModStructurePieceTypes() {}
}
