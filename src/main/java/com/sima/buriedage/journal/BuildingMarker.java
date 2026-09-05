package com.sima.buriedage.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What a building marker item carries and what its block entity keeps: the building id and the trigger radius. */
public record BuildingMarker(String building, int radius) {
    public static final int DEFAULT_RADIUS = 6;
    public static final BuildingMarker EMPTY = new BuildingMarker("", DEFAULT_RADIUS);

    public static final Codec<BuildingMarker> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.STRING.optionalFieldOf("building", "").forGetter(BuildingMarker::building),
                    Codec.INT.optionalFieldOf("radius", DEFAULT_RADIUS).forGetter(BuildingMarker::radius))
            .apply(i, BuildingMarker::new));

    public static final StreamCodec<ByteBuf, BuildingMarker> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BuildingMarker::building,
            ByteBufCodecs.VAR_INT, BuildingMarker::radius,
            BuildingMarker::new);

    public boolean isSet() {
        return !this.building.isBlank();
    }

    /** "greek/temple" becomes "greek". An id without a slash forms a group of its own. */
    public static String groupOf(String building) {
        int slash = building.indexOf('/');
        return slash < 0 ? building : building.substring(0, slash);
    }
}
