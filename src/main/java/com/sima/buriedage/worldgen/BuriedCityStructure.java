package com.sima.buriedage.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

public class BuriedCityStructure extends Structure {
    public static final MapCodec<BuriedCityStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Structure.settingsCodec(i),
                    JigsawStructure.CODEC.forGetter(s -> s.jigsaw))
            .apply(i, BuriedCityStructure::new));

    private static final Identifier HINT = Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "surface_hint");
    private static final Identifier AGORA = Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "city/agora");
    private static final BlockState COLUMN = Blocks.QUARTZ_PILLAR.defaultBlockState();
    private static final BlockState[][] STUMP = {
            { Blocks.CALCITE.defaultBlockState(), COLUMN, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), null },
            { null, Blocks.DIORITE_WALL.defaultBlockState().setValue(WallBlock.UP, true), null, null },
    };

    private static final int COLUMN_GAP = 2;
    private static final int COVER = 3;
    private static final int SAMPLE_STEP = 8;
    private static final int MAX_SINK = 16;
    private static final int BURIAL_MARGIN = 5;
    private static final int WATER_TOLERANCE = 1;

    private final JigsawStructure jigsaw;

    public BuriedCityStructure(Structure.StructureSettings settings, JigsawStructure jigsaw) {
        super(settings);
        this.jigsaw = jigsaw;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        return this.jigsaw.findValidGenerationPoint(context).flatMap(stub -> {
            List<BuriedCityPiece> pieces = new ArrayList<>();
            for (StructurePiece piece : stub.getPiecesBuilder().build().pieces()) {
                if (piece instanceof PoolElementStructurePiece pool) {
                    pieces.add(new BuriedCityPiece(context.structureTemplateManager(), pool));
                }
            }

            if (!settle(context, pieces)) {
                return Optional.empty();
            }

            return Optional.of(new Structure.GenerationStub(stub.position(), builder -> pieces.forEach(builder::addPiece)));
        });
    }

    private static boolean settle(Structure.GenerationContext context, List<BuriedCityPiece> pieces) {
        long started = System.nanoTime();
        Map<Long, Integer> ground = new HashMap<>();
        if (standsInWater(context, ground, pieces)) {
            TheBuriedAge.LOGGER.debug("[city] water here, {} columns sampled in {} ms", ground.size(), (System.nanoTime() - started) / 1000000L);
            return false;
        }

        int sink = 0;
        BuriedCityPiece hint = null;
        BuriedCityPiece agora = null;
        for (BuriedCityPiece piece : pieces) {
            if (!piece.isRigid()) {
                if (isTemplate(piece, HINT)) {
                    hint = piece;
                }

                continue;
            }

            if (isTemplate(piece, AGORA)) {
                agora = piece;
            }

            BoundingBox box = piece.getBoundingBox();
            int lowestOver = lowestGround(context, ground, box);
            sink = Math.max(sink, box.maxY() + COVER - lowestOver);
            sink = Math.max(sink, box.minY() + BuriedCityPiece.BURIAL_REACH - lowestOver);
        }

        if (sink > MAX_SINK) {
            TheBuriedAge.LOGGER.debug("[city] the ground asks for {} blocks, more than {}, so no city ({} columns, {} ms)", sink, MAX_SINK, ground.size(), (System.nanoTime() - started) / 1000000L);
            return false;
        }

        if (sink > 0) {
            TheBuriedAge.LOGGER.debug("[city] sinking the city {} blocks ({} columns, {} ms)", sink, ground.size(), (System.nanoTime() - started) / 1000000L);
            for (BuriedCityPiece piece : pieces) {
                if (piece.isRigid()) {
                    piece.move(0, -sink, 0);
                }
            }
        }

        if (hint != null && agora != null) {
            BoundingBox box = agora.getBoundingBox();
            int x = box.maxX() + 1 + COLUMN_GAP;
            int z = (box.minZ() + box.maxZ()) / 2;
            int top = Integer.MIN_VALUE;
            for (int dx = 0; dx < 2; dx++) {
                for (int dz = 0; dz < 2; dz++) {
                    top = Math.max(top, context.chunkGenerator().getFirstOccupiedHeight(x + dx, z + dz,
                            Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()));
                }
            }

            BoundingBox old = hint.getBoundingBox();
            hint.move(x - old.minX(), top + 1 - old.minY(), z - old.minZ());
        }

        return true;
    }

    /**
     * A lake over the city drowns the whole idea: the pieces would sit in the water rather than
     * under soil, and the burial pass would pile a rectangular island onto the surface. Any real
     * water over the footprint sends the city somewhere else.
     */
    private static boolean standsInWater(Structure.GenerationContext context, Map<Long, Integer> ground,
                                         List<BuriedCityPiece> pieces) {
        BoundingBox footprint = null;
        for (BuriedCityPiece piece : pieces) {
            if (piece.isRigid()) {
                BoundingBox box = piece.getBoundingBox();
                footprint = footprint == null ? box : BoundingBox.encapsulatingBoxes(List.of(footprint, box)).orElse(box);
            }
        }

        if (footprint == null) {
            return false;
        }

        BoundingBox around = footprint.inflatedBy(BURIAL_MARGIN, 0, BURIAL_MARGIN);
        for (int x = around.minX(); x <= around.maxX(); x += SAMPLE_STEP) {
            for (int z = around.minZ(); z <= around.maxZ(); z += SAMPLE_STEP) {
                int floor = groundAt(context, ground, x, z);
                int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z,
                        Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (surface - floor > WATER_TOLERANCE) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int groundAt(Structure.GenerationContext context, Map<Long, Integer> cache, int x, int z) {
        return cache.computeIfAbsent(BlockPos.asLong(x, 0, z), key -> context.chunkGenerator()
                .getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
    }

    private static boolean isTemplate(PoolElementStructurePiece piece, Identifier template) {
        return piece.getElement() instanceof SinglePoolElement single && single.getTemplateLocation().equals(template);
    }

    /**
     * The lowest ground the box actually stands over. Only columns inside the box count: a ravine a
     * few blocks past its corner says nothing about whether this piece is covered, and letting one
     * in used to drag the whole city down after it.
     */
    private static int lowestGround(Structure.GenerationContext context, Map<Long, Integer> cache, BoundingBox box) {
        int lowest = Integer.MAX_VALUE;
        for (int x : new int[] { box.minX(), box.maxX() }) {
            for (int z : new int[] { box.minZ(), box.maxZ() }) {
                lowest = Math.min(lowest, groundAt(context, cache, x, z));
            }
        }

        int firstX = Math.floorDiv(box.minX() + SAMPLE_STEP - 1, SAMPLE_STEP) * SAMPLE_STEP;
        int firstZ = Math.floorDiv(box.minZ() + SAMPLE_STEP - 1, SAMPLE_STEP) * SAMPLE_STEP;
        for (int x = firstX; x <= box.maxX(); x += SAMPLE_STEP) {
            for (int z = firstZ; z <= box.maxZ(); z += SAMPLE_STEP) {
                lowest = Math.min(lowest, groundAt(context, cache, x, z));
            }
        }

        return lowest;
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
            if (!(piece instanceof PoolElementStructurePiece pool)) {
                continue;
            }

            if (isTemplate(pool, HINT)) {
                hint = pool.getBoundingBox();
            } else if (isTemplate(pool, AGORA)) {
                agora = pool.getBoundingBox();
            }
        }

        if (hint == null || agora == null) {
            return;
        }

        int base = hint.minY();
        int bottom = agora.maxY() + 1;
        if (base <= bottom || hint.maxX() < chunkBB.minX() || hint.minX() > chunkBB.maxX()
                || hint.maxZ() < chunkBB.minZ() || hint.minZ() > chunkBB.maxZ()) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                int x = hint.minX() + dx;
                int z = hint.minZ() + dz;
                for (int y = base - 1; y >= bottom; y--) {
                    pos.set(x, y, z);
                    if (chunkBB.isInside(pos)) {
                        level.setBlock(pos, COLUMN, 2);
                    }
                }

                for (int layer = 0; layer < STUMP.length; layer++) {
                    BlockState state = STUMP[layer][dx * 2 + dz];
                    pos.set(x, base + layer, z);
                    if (state != null && chunkBB.isInside(pos)) {
                        level.setBlock(pos, state, 2);
                    }
                }
            }
        }
    }
}
