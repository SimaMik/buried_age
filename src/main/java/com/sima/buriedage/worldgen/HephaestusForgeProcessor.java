package com.sima.buriedage.worldgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import com.sima.buriedage.block.HephaestusForgeBlock;
import com.sima.buriedage.registry.ModProcessors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class HephaestusForgeProcessor extends StructureProcessor {
    public static final HephaestusForgeProcessor INSTANCE = new HephaestusForgeProcessor();
    public static final MapCodec<HephaestusForgeProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    private HephaestusForgeProcessor() {}

    @Override
    protected StructureProcessorType<?> getType() {
        return ModProcessors.HEPHAESTUS_FORGE.get();
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position,
            BlockPos referencePos, List<StructureTemplate.StructureBlockInfo> originalBlockInfoList,
            List<StructureTemplate.StructureBlockInfo> processedBlockInfoList, StructurePlaceSettings settings) {
        List<StructureTemplate.StructureBlockInfo> uppers = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfoList) {
            if (HephaestusForgeBlock.isLower(info.state())) {
                uppers.add(new StructureTemplate.StructureBlockInfo(info.pos().above(),
                        info.state().setValue(HephaestusForgeBlock.HALF, DoubleBlockHalf.UPPER), null));
            }
        }

        if (uppers.isEmpty()) {
            return processedBlockInfoList;
        }

        Set<BlockPos> taken = new HashSet<>();
        for (StructureTemplate.StructureBlockInfo upper : uppers) {
            taken.add(upper.pos());
        }

        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>(processedBlockInfoList.size() + uppers.size());
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfoList) {
            if (!taken.contains(info.pos())) {
                result.add(info);
            }
        }

        result.addAll(uppers);
        return result;
    }
}
