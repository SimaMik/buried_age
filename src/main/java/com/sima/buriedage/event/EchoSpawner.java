package com.sima.buriedage.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sima.buriedage.TheBuriedAge;
import com.sima.buriedage.entity.EchoEntity;
import com.sima.buriedage.entity.EchoTuning;
import com.sima.buriedage.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Decides when the buried city shows somebody who used to live in it. Runs off the server tick and
 * asks one question: is this player standing inside the structure? Nothing is placed in the
 * templates, and no marker blocks are involved.
 *
 * <p>All numbers live in {@link EchoTuning}, including the switch that turns this off entirely.
 */
@EventBusSubscriber(modid = TheBuriedAge.MODID)
public final class EchoSpawner {
    private static final ResourceKey<Structure> BURIED_CITY = ResourceKey.create(
            Registries.STRUCTURE, Identifier.fromNamespaceAndPath(TheBuriedAge.MODID, "buried_city"));

    /** Ticks until each player may roll again. */
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

    private EchoSpawner() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!EchoTuning.AUTO_SPAWN_ENABLED) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        int remaining = COOLDOWNS.getOrDefault(id, 0) - 1;
        if (remaining > 0) {
            COOLDOWNS.put(id, remaining);
            return;
        }

        ServerLevel level = player.level();
        RandomSource random = level.getRandom();

        // Not in the city: park the roll for one interval instead of checking every tick.
        if (!isInsideCity(level, player.blockPosition())) {
            COOLDOWNS.put(id, EchoTuning.ROLL_INTERVAL);
            return;
        }

        if (random.nextFloat() >= EchoTuning.SPAWN_CHANCE) {
            COOLDOWNS.put(id, EchoTuning.ROLL_INTERVAL);
            return;
        }

        if (countEchoes(level) >= EchoTuning.MAX_ECHOES) {
            COOLDOWNS.put(id, EchoTuning.ROLL_INTERVAL);
            return;
        }

        boolean spawned = spawnGroup(level, player, random);
        COOLDOWNS.put(id, spawned
                ? EchoTuning.randomBetween(random, EchoTuning.COOLDOWN_MIN, EchoTuning.COOLDOWN_MAX)
                : EchoTuning.ROLL_INTERVAL);
    }

    /** Same question the root advancement asks: does this position sit inside a city piece? */
    private static boolean isInsideCity(ServerLevel level, BlockPos pos) {
        Holder<Structure> city = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .get(BURIED_CITY).orElse(null);
        if (city == null) {
            return false;
        }

        return level.structureManager().getStructureWithPieceAt(pos, holder -> holder == city).isValid();
    }

    private static int countEchoes(ServerLevel level) {
        return level.getEntities(ModEntities.ECHO.get(),
                new AABB(BlockPos.ZERO).inflate(3.0E7), echo -> true).size();
    }

    /** Rolls a mode and places one echo, two independent ones, or a meeting pair. */
    private static boolean spawnGroup(ServerLevel level, ServerPlayer player, RandomSource random) {
        int roll = random.nextInt(EchoTuning.WEIGHT_DRIFT + EchoTuning.WEIGHT_WANDER + EchoTuning.WEIGHT_MEETING);
        if (roll < EchoTuning.WEIGHT_MEETING) {
            return spawnMeeting(level, player, random);
        }

        boolean any = spawnSingle(level, player, random,
                roll < EchoTuning.WEIGHT_MEETING + EchoTuning.WEIGHT_DRIFT
                        ? EchoEntity.Mode.DRIFT : EchoEntity.Mode.WANDER);

        // Sometimes the city feels inhabited rather than haunted by exactly one person.
        if (any && random.nextFloat() < EchoTuning.DOUBLE_SPAWN_CHANCE
                && countEchoes(level) < EchoTuning.MAX_ECHOES) {
            spawnSingle(level, player, random,
                    random.nextBoolean() ? EchoEntity.Mode.DRIFT : EchoEntity.Mode.WANDER);
        }

        return any;
    }

    private static boolean spawnSingle(ServerLevel level, ServerPlayer player, RandomSource random, EchoEntity.Mode mode) {
        Vec3 spot = findSpot(level, player.position(), random);
        if (spot == null) {
            return false;
        }

        float angle = random.nextFloat() * Mth.TWO_PI;
        Vec3 heading = new Vec3(Mth.cos(angle), 0.0, Mth.sin(angle));
        return place(level, spot, heading, mode, random) != null;
    }

    private static boolean spawnMeeting(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (countEchoes(level) + 2 > EchoTuning.MAX_ECHOES) {
            return false;
        }

        Vec3 first = findSpot(level, player.position(), random);
        if (first == null) {
            return false;
        }

        double separation = EchoTuning.randomBetween(random, EchoTuning.MEETING_SEPARATION_MIN, EchoTuning.MEETING_SEPARATION_MAX);
        float angle = random.nextFloat() * Mth.TWO_PI;
        Vec3 axis = new Vec3(Mth.cos(angle), 0.0, Mth.sin(angle));
        Vec3 second = first.add(axis.scale(separation));
        if (!isInsideCity(level, BlockPos.containing(second))) {
            return false;
        }

        Vec3 midpoint = first.add(second).scale(0.5);
        EchoEntity a = place(level, first, axis, EchoEntity.Mode.MEETING, random);
        EchoEntity b = place(level, second, axis.reverse(), EchoEntity.Mode.MEETING, random);
        if (a == null || b == null) {
            return false;
        }

        a.configureMeeting(midpoint, b);
        b.configureMeeting(midpoint, a);
        return true;
    }

    private static @Nullable EchoEntity place(ServerLevel level, Vec3 at, Vec3 heading, EchoEntity.Mode mode, RandomSource random) {
        EchoEntity echo = ModEntities.ECHO.get().create(level, EntitySpawnReason.TRIGGERED);
        if (echo == null) {
            return null;
        }

        echo.snapTo(at.x, at.y, at.z);
        echo.configure(mode, heading, random.nextInt(EchoTuning.PROFESSIONS.length));
        if (!level.addFreshEntity(echo)) {
            return null;
        }

        echo.playArrivalSound(level);
        return echo;
    }

    /**
     * Somewhere in the ring around the player that is still inside the city and has headroom.
     * Echoes pass through blocks, but one that starts buried in stone is a wasted spawn.
     */
    private static @Nullable Vec3 findSpot(ServerLevel level, Vec3 playerPos, RandomSource random) {
        // Best case is open space the player can see into. In a city that is still mostly solid
        // rock there is almost none, so remember any spot inside the structure as a fallback:
        // an echo stepping out of the stone reads better than no echo at all, and it walks
        // through blocks anyway.
        Vec3 fallback = null;

        for (int attempt = 0; attempt < 12; attempt++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            double distance = EchoTuning.randomBetween(random, EchoTuning.SPAWN_RADIUS_MIN, EchoTuning.SPAWN_RADIUS_MAX);
            double x = playerPos.x + Mth.cos(angle) * distance;
            double z = playerPos.z + Mth.sin(angle) * distance;

            for (int dy = 0; dy <= EchoTuning.SPAWN_VERTICAL_SEARCH; dy++) {
                for (int sign : new int[] { 1, -1 }) {
                    double y = playerPos.y + dy * sign;
                    BlockPos pos = BlockPos.containing(x, y, z);
                    if (!isInsideCity(level, pos)) {
                        if (dy == 0) {
                            break;
                        }

                        continue;
                    }

                    if (hasRoom(level, pos)) {
                        return new Vec3(x, y, z);
                    }

                    if (fallback == null) {
                        fallback = new Vec3(x, y, z);
                    }

                    if (dy == 0) {
                        break;
                    }
                }
            }
        }

        return fallback;
    }

    private static boolean hasRoom(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    /** Player left: drop their cooldown so the map cannot grow without bound. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        COOLDOWNS.remove(event.getEntity().getUUID());
    }
}
