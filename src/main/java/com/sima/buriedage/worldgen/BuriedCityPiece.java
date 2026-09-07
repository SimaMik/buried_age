package com.sima.buriedage.worldgen;

import com.sima.buriedage.registry.ModStructurePieceTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.common.world.PieceBeardifierModifier;

public class BuriedCityPiece extends PoolElementStructurePiece implements PieceBeardifierModifier {
    public static final int BURIAL_DROP = 5;
    public static final int BURIAL_REACH = 12 - BURIAL_DROP;

    public BuriedCityPiece(StructureTemplateManager templates, PoolElementStructurePiece source) {
        super(templates, source.getElement(), source.getPosition(), source.getGroundLevelDelta(),
                source.getRotation(), source.getBoundingBox(), JigsawStructure.DEFAULT_LIQUID_SETTINGS);
        source.getJunctions().forEach(this::addJunction);
    }

    public BuriedCityPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(context, tag);
    }

    @Override
    public StructurePieceType getType() {
        return ModStructurePieceTypes.CITY_PIECE.get();
    }

    public boolean isRigid() {
        return this.getElement().getProjection() == StructureTemplatePool.Projection.RIGID;
    }

    @Override
    public BoundingBox getBeardifierBox() {
        return this.getBoundingBox().moved(0, -BURIAL_DROP, 0);
    }

    @Override
    public TerrainAdjustment getTerrainAdjustment() {
        return this.isRigid() ? TerrainAdjustment.BURY : TerrainAdjustment.NONE;
    }
}
