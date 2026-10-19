package com.iury.minspawn8.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Persistent config: global minimum spawn distance (hostile/non-hostile) + player-defined
 * boost regions (also split hostile/non-hostile). Stored as plain JSON in config/minspawn8.json
 * so it survives restarts.
 */
public final class Minspawn8Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("minspawn8.json");
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private static Data data = load();

    private Minspawn8Config() {
    }

    public static final class Data {
        public int minSpawnDistanceHostile = 8;
        public int minSpawnDistanceNonHostile = 8;
        public List<Region> regions = new ArrayList<>();
    }

    /** The three independent spawn-rate groups a region can tune. */
    public enum Bucket {
        HOSTILE, NON_HOSTILE, MISC
    }

    public static final class Region {
        public int id;
        public String dimension;
        public int x1, y1, z1;
        public int x2, y2, z2;
        public double percentHostile = 100.0;
        public double percentNonHostile = 100.0;
        /** Rate for MISC-category production that is not natural spawning, e.g. iron golems made by villagers. */
        public double percentMisc = 100.0;
        /** Entity type ids (e.g. "minecraft:zombie") allowed to spawn naturally inside; empty = no restriction. */
        public List<String> allowedMobs = new ArrayList<>();
        /**
         * Strict mode: besides natural spawns, every mob (and every non-vanilla entity such as a corpse
         * from another mod) that is not on the list is refused when it is added to the level in here.
         */
        public boolean strict = false;

        /**
         * True if the position is inside the selected square on X/Z, at any height. The two clicked
         * blocks usually sit at the same Y (e.g. both on the floor), so using Y would make the box
         * one block tall and let almost every spawn escape the mob list.
         */
        public double percent(Bucket bucket) {
            return switch (bucket) {
                case HOSTILE -> percentHostile;
                case NON_HOSTILE -> percentNonHostile;
                case MISC -> percentMisc;
            };
        }

        boolean containsColumn(BlockPos pos) {
            return pos.getX() >= Math.min(x1, x2) && pos.getX() <= Math.max(x1, x2)                    && pos.getZ() >= Math.min(z1, z2) && pos.getZ() <= Math.max(z1, z2);
        }

        /** True if the chunk's X/Z column overlaps the region's own (inner) selection. */
        boolean chunkInInner(int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
            return chunkMinX <= maxX && chunkMaxX >= minX && chunkMinZ <= maxZ && chunkMaxZ >= minZ;
        }

        /**
         * True if the chunk falls in the compensation ring surrounding the region: an area as wide/deep
         * as the region itself, immediately outside it, but not in the inner selection.
         */
        boolean chunkInRing(int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
            int width = maxX - minX + 1;
            int depth = maxZ - minZ + 1;
            int outerMinX = minX - width, outerMaxX = maxX + width;
            int outerMinZ = minZ - depth, outerMaxZ = maxZ + depth;
            boolean inOuter = chunkMinX <= outerMaxX && chunkMaxX >= outerMinX && chunkMinZ <= outerMaxZ && chunkMaxZ >= outerMinZ;
            return inOuter && !chunkInInner(chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
    }

    private static synchronized Data load() {
        try {
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                    Data loaded = GSON.fromJson(reader, Data.class);
                    if (loaded != null) {
                        if (loaded.regions == null) {
                            loaded.regions = new ArrayList<>();
                        }
                        int maxId = 0;
                        for (Region region : loaded.regions) {
                            maxId = Math.max(maxId, region.id);
                            if (region.allowedMobs == null) {
                                region.allowedMobs = new ArrayList<>();
                            }
                        }
                        NEXT_ID.set(maxId + 1);
                        return loaded;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load minspawn8.json", e);
        }
        return new Data();
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save minspawn8.json", e);
        }
    }

    public static synchronized int getMinSpawnDistanceHostile() {
        return data.minSpawnDistanceHostile;
    }

    public static synchronized void setMinSpawnDistanceHostile(int blocks) {
        data.minSpawnDistanceHostile = blocks;
        save();
    }

    public static synchronized int getMinSpawnDistanceNonHostile() {
        return data.minSpawnDistanceNonHostile;
    }

    public static synchronized void setMinSpawnDistanceNonHostile(int blocks) {
        data.minSpawnDistanceNonHostile = blocks;
        save();
    }

    public static synchronized List<Region> getRegions() {
        return new ArrayList<>(data.regions);
    }

    public static synchronized Region createRegion(ResourceLocation dimensionKey, BlockPos pos1, BlockPos pos2) {
        Region region = new Region();
        region.id = NEXT_ID.getAndIncrement();
        region.dimension = dimensionKey.toString();
        region.x1 = pos1.getX();
        region.y1 = pos1.getY();
        region.z1 = pos1.getZ();
        region.x2 = pos2.getX();
        region.y2 = pos2.getY();
        region.z2 = pos2.getZ();
        data.regions.add(region);
        save();
        return region;
    }

    public static synchronized boolean setRegionPercentHostile(int id, double percent) {
        for (Region region : data.regions) {
            if (region.id == id) {
                region.percentHostile = percent;
                save();
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean setRegionPercentNonHostile(int id, double percent) {
        for (Region region : data.regions) {
            if (region.id == id) {
                region.percentNonHostile = percent;
                save();
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean setRegionPercent(int id, Bucket bucket, double percent) {
        for (Region region : data.regions) {
            if (region.id == id) {
                switch (bucket) {
                    case HOSTILE -> region.percentHostile = percent;
                    case NON_HOSTILE -> region.percentNonHostile = percent;
                    case MISC -> region.percentMisc = percent;
                }
                save();
                return true;
            }
        }
        return false;
    }

    /** Returns null if the region does not exist, otherwise a copy of its allowed-mob list. */
    public static synchronized List<String> getAllowedMobs(int id) {
        for (Region region : data.regions) {
            if (region.id == id) {
                return new ArrayList<>(region.allowedMobs);
            }
        }
        return null;
    }

    /** @return false if the region does not exist; true otherwise (also when the mob was already listed). */
    public static synchronized boolean addAllowedMob(int id, String mobId) {
        for (Region region : data.regions) {
            if (region.id == id) {
                if (!region.allowedMobs.contains(mobId)) {
                    region.allowedMobs.add(mobId);
                    save();
                }
                return true;
            }
        }
        return false;
    }

    /** @return 1 if removed, 0 if the mob was not in the list, -1 if the region does not exist. */
    public static synchronized int removeAllowedMob(int id, String mobId) {
        for (Region region : data.regions) {
            if (region.id == id) {
                boolean removed = region.allowedMobs.remove(mobId);
                if (removed) {
                    save();
                }
                return removed ? 1 : 0;
            }
        }
        return -1;
    }

    public static synchronized boolean clearAllowedMobs(int id) {
        for (Region region : data.regions) {
            if (region.id == id) {
                if (!region.allowedMobs.isEmpty()) {
                    region.allowedMobs.clear();
                    save();
                }
                return true;
            }
        }
        return false;
    }

    /** @return false if the region does not exist. */
    public static synchronized boolean setRegionStrict(int id, boolean strict) {
        for (Region region : data.regions) {
            if (region.id == id) {
                region.strict = strict;
                save();
                return true;
            }
        }
        return false;
    }

    /**
     * Strict mode check. True if some strict region covers the position and none of the strict regions
     * covering it lists the entity.
     */
    public static synchronized boolean isBlockedByStrictRegion(String dimensionKey, BlockPos pos, String entityId) {
        boolean restricted = false;
        for (Region region : data.regions) {
            if (!region.strict || !region.dimension.equals(dimensionKey) || !region.containsColumn(pos)) {
                continue;
            }
            restricted = true;
            if (region.allowedMobs.contains(entityId)) {
                return false;
            }
        }
        return restricted;
    }

    /**
     * Natural-spawn whitelist. Looks at every region whose box contains the position and that has a
     * non-empty mob list: if there is none the spawn is unrestricted; otherwise the mob must be in
     * the list of at least one of them.
     */
    public static synchronized boolean isMobAllowedAt(String dimensionKey, BlockPos pos, String mobId) {
        boolean restricted = false;
        for (Region region : data.regions) {
            if (region.allowedMobs.isEmpty() || !region.dimension.equals(dimensionKey) || !region.containsColumn(pos)) {
                continue;
            }
            restricted = true;
            if (region.allowedMobs.contains(mobId)) {
                return true;
            }
        }
        return !restricted;
    }

    public static synchronized boolean removeRegion(int id) {
        boolean removed = data.regions.removeIf(region -> region.id == id);
        if (removed) {
            save();
        }
        return removed;
    }

    /**
     * Natural spawning is processed per-chunk (NaturalSpawner.spawnForChunk), so region effects are
     * applied at chunk (column) granularity: the region's Y bounds don't restrict where within the
     * chunk a mob spawns, only its X/Z range does.
     * <p>
     * Inside the region: multiplier = percent / 100 (for the matching hostile/non-hostile bucket).
     * In a surrounding ring as wide/deep as the region itself: multiplier is mirrored around 1.0
     * (e.g. +50% inside becomes -50% in the ring, and vice-versa), clamped at 0.
     */
    public static synchronized double getChunkSpawnMultiplier(String dimensionKey, ChunkPos chunkPos, boolean hostile) {
        return getChunkSpawnMultiplier(dimensionKey, chunkPos, hostile ? Bucket.HOSTILE : Bucket.NON_HOSTILE);
    }

    public static synchronized double getChunkSpawnMultiplier(String dimensionKey, ChunkPos chunkPos, Bucket bucket) {
        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMaxX = chunkPos.getMaxBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int chunkMaxZ = chunkPos.getMaxBlockZ();

        double bestDeviation = 0.0;
        double bestMultiplier = 1.0;

        for (Region region : data.regions) {
            if (!region.dimension.equals(dimensionKey) || !region.chunkInInner(chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ)) {
                continue;
            }
            double percent = region.percent(bucket);
            double multiplier = percent / 100.0;
            double deviation = Math.abs(multiplier - 1.0);
            if (deviation > bestDeviation) {
                bestDeviation = deviation;
                bestMultiplier = multiplier;
            }
        }
        if (bestDeviation > 0.0) {
            return bestMultiplier;
        }

        for (Region region : data.regions) {
            if (!region.dimension.equals(dimensionKey) || !region.chunkInRing(chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ)) {
                continue;
            }
            double percent = region.percent(bucket);
            double innerMultiplier = percent / 100.0;
            double ringMultiplier = Math.max(0.0, 2.0 - innerMultiplier);
            double deviation = Math.abs(ringMultiplier - 1.0);
            if (deviation > bestDeviation) {
                bestDeviation = deviation;
                bestMultiplier = ringMultiplier;
            }
        }
        return bestMultiplier;
    }
}
