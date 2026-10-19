package com.iury.minspawn8.mixin;

import com.iury.minspawn8.config.Minspawn8Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Applies a region's MISC rate to the iron golems that villagers make (Villager#spawnGolemIfNeeded).
 * 100% = vanilla. 300% = the villager's spawn attempt runs 3 times (2 extra golems per success),
 * 150% = 1 golem + 50% chance of a 2nd. Below 100% the golem is skipped with probability (1 - rate),
 * but the villagers still count it as spawned, so they do not retry right away.
 */
@Mixin(value = Villager.class, remap = false)
public abstract class VillagerGolemMixin {

    @Redirect(
        method = "spawnGolemIfNeeded",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/SpawnUtil;trySpawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/util/SpawnUtil$Strategy;)Ljava/util/Optional;"
        ),
        remap = false,
        require = 1,
        allow = 1
    )
    private Optional minspawn8$applyMiscRate(
            EntityType type,
            MobSpawnType spawnType,
            ServerLevel level,
            BlockPos pos,
            int attempts,
            int spread,
            int yOffset,
            SpawnUtil.Strategy strategy
    ) {
        double multiplier = Minspawn8Config.getChunkSpawnMultiplier(
                level.dimension().location().toString(), new ChunkPos(pos), Minspawn8Config.Bucket.MISC);

        if (multiplier < 1.0D && level.getRandom().nextDouble() >= multiplier) {
            // Pretend it spawned (an entity that is never added to the level) so the villagers do not retry.
            return Optional.ofNullable(type.create(level));
        }

        Optional result = SpawnUtil.trySpawnMob(type, spawnType, level, pos, attempts, spread, yOffset, strategy);
        if (multiplier > 1.0D && result.isPresent()) {
            double extra = multiplier - 1.0D;
            int extraCalls = (int) Math.floor(extra);
            if (level.getRandom().nextDouble() < extra - extraCalls) {
                extraCalls++;
            }
            for (int i = 0; i < extraCalls; i++) {
                SpawnUtil.trySpawnMob(type, spawnType, level, pos, attempts, spread, yOffset, strategy);
            }
        }
        return result;
    }
}
