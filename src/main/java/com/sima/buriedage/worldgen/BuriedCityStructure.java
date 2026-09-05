package com.sima.buriedage.worldgen;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.registry.ModStructureTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

/**
 * The buried city is a plain jigsaw structure with one addition: after the pieces are placed,
 * the marble stump on the surface is continued straight down through the ground until it meets
 * the roof of the agora, so what pokes out of the grass reads as the tip of a real column and
 * digging along it leads into the city. Jigsaw cannot do this on its own: a shaft piece would
 * either collide with the agora or stop short of it, depending on how deep the city sits.
 */
public class BuriedCityStructure extends Structure {
    public static final MapCodec<BuriedCityStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Structure.settingsCodec(i),
                    JigsawStructure.CODEC.forGetter(s -> s.jigsaw))
            .apply(i, BuriedCityStructure::new));

    private static final Identifier HINT = Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "surface_hint");
    private static final Identifier AGORA = Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "city/agora");
    private static final BlockState COLUMN = Blocks.QUARTZ_PILLAR.defaultBlockState();

    private final JigsawStructure jigsaw;

    public BuriedCityStructure(Structure.StructureSettings settings, JigsawStructure jigsaw) {
        super(settings);
        this.jigsaw = jigsaw;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        return this.jigsaw.findValidGenerationPoint(context);
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.BURIED_CITY.get();
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                           RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, PiecesContainer pieces) {
        BoundingBox hint = null;
        BoundingBox agora = null;
        for (StructurePiece piece : pieces.pieces()) {
            if (!(piece instanceof PoolElementStructurePiece pool) || !(pool.getElement() instanceof SinglePoolElement single)) {
                continue;
            }
            Identifier template = single.getTemplateLocation();
            if (template.equals(HINT)) {
                hint = pool.getBoundingBox();
            } else if (template.equals(AGORA)) {
                agora = pool.getBoundingBox();
            }
        }
        if (hint == null || agora == null) {
            return;
        }

        int top = hint.minY() - 1;
        int bottom = agora.maxY() + 1;
        if (top < bottom || hint.maxX() < chunkBB.minX() || hint.minX() > chunkBB.maxX()
                || hint.maxZ() < chunkBB.minZ() || hint.minZ() > chunkBB.maxZ()) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = hint.minX(); x <= hint.maxX(); x++) {
            for (int z = hint.minZ(); z <= hint.maxZ(); z++) {
                for (int y = top; y >= bottom; y--) {
                    pos.set(x, y, z);
                    if (chunkBB.isInside(pos)) {
                        level.setBlock(pos, COLUMN, 2);
                    }
                }
            }
        }
    }
}
