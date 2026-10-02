package com.iury.minspawn8.mixin;

import com.iury.minspawn8.config.Minspawn8Config;
import com.iury.minspawn8.util.SpawnCategoryContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Server-side patch for Minecraft 1.21.1 / NeoForge 21.1.x.
 * Vanilla natural spawning rejects positions closer than 24 blocks (24^2 = 576).
 * This reads the minimum distance from the mod config instead (separately for hostile and
 * non-hostile mobs, default 8 blocks each), configurable at runtime via /minspawn8 distance.
 * This private method doesn't receive the MobCategory being spawned, so NaturalSpawnerRegionMixin
 * records it in SpawnCategoryContext right before calling down into this code path.
 */
@Mixin(targets = "net.minecraft.world.level.NaturalSpawner", remap = false)
public abstract class NaturalSpawnerMixin {
    @ModifyConstant(
        method = {"isRightDistanceToPlayerAndSpawnPoint", "m_47024_"},
        constant = @Constant(doubleValue = 576.0D),
        remap = false,
        require = 1,
        allow = 1
    )
    private static double minspawn8$changeMinimumNaturalSpawnDistance(double original) {
        int blocks = SpawnCategoryContext.isHostile()
                ? Minspawn8Config.getMinSpawnDistanceHostile()
                : Minspawn8Config.getMinSpawnDistanceNonHostile();
        return (double) blocks * blocks;
    }
}
