package com.sima.buriedage.block.entity;

import com.sima.buriedage.journal.BuildingMarker;
import com.sima.buriedage.journal.JournalService;
import com.sima.buriedage.registry.ModBlockEntities;
import com.sima.buriedage.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps the building id and the radius. Both round-trip through the item component (so /give works)
 * and through plain NBT (so a structure block saves them into the template).
 */
public class BuildingMarkerBlockEntity extends BlockEntity {
    private static final int CHECK_INTERVAL = 20;

    private String building = "";
    private int radius = BuildingMarker.DEFAULT_RADIUS;
    private int untilCheck;

    public BuildingMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUILDING_MARKER.get(), pos, state);
    }

    public BuildingMarker marker() {
        return new BuildingMarker(this.building, this.radius);
    }

    public void serverTick(ServerLevel level) {
        if (++this.untilCheck < CHECK_INTERVAL) {
            return;
        }
        this.untilCheck = 0;
        if (this.building.isBlank()) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        double reach = (double) this.radius * this.radius;
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(center) <= reach) {
                JournalService.visitBuilding(player, this.building);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.building = input.getStringOr("building", "");
        this.radius = input.getIntOr("radius", BuildingMarker.DEFAULT_RADIUS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("building", this.building);
        output.putInt("radius", this.radius);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        BuildingMarker marker = components.get(ModDataComponents.BUILDING_MARKER.get());
        if (marker != null) {
            this.building = marker.building();
            this.radius = marker.radius();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.BUILDING_MARKER.get(), this.marker());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("building");
        output.discard("radius");
    }
}
