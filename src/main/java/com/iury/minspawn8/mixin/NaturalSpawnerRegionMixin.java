package com.iury.minspawn8.mixin;

import com.iury.minspawn8.config.Minspawn8Config;
import com.iury.minspawn8.util.SpawnCategoryContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Multiplies natural spawn attempts for chunks that fall inside a player-marked region
 * (see RegionWandEvents / Minspawn8Config), by repeating NaturalSpawner's per-category,
 * per-chunk spawn pass according to the region's configured percent.
 * 100% = vanilla (1 pass). 250% = 2 guaranteed passes + 50% chance of a 3rd.
 */
@Mixin(value = NaturalSpawner.class, remap = false)
public abstract class NaturalSpawnerRegionMixin {

    @Redirect(
        method = "spawnForChunk",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner;spawnCategoryForChunk(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V"
        ),
        remap = false,
        require = 1,
        allow = 1
    )
    private static void minspawn8$applyRegionMultiplier(
            MobCategory category,
            ServerLevel level,
            LevelChunk chunk,
            NaturalSpawner.SpawnPredicate checker,
            NaturalSpawner.AfterSpawnCallback runner
    ) {
        boolean hostile = !category.isFriendly();
        SpawnCategoryContext.setHostile(hostile);

        double multiplier = Minspawn8Config.getChunkSpawnMultiplier(
                level.dimension().location().toString(), chunk.getPos(), hostile);

        int fullCalls = (int) Math.floor(multiplier);
        double remainder = multiplier - fullCalls;

        for (int i = 0; i < fullCalls; i++) {
            NaturalSpawner.spawnCategoryForChunk(category, level, chunk, checker, runner);
        }
        if (remainder > 0.0D && level.getRandom().nextDouble() < remainder) {
            NaturalSpawner.spawnCategoryForChunk(category, level, chunk, checker, runner);
        }
    }
}
