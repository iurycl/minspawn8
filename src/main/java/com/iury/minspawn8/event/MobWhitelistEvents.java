package com.iury.minspawn8.event;

import com.iury.minspawn8.config.Minspawn8Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Enforces the per-region mob list: inside a region that has a list, natural spawns of any mob
 * that is not on it are refused before the mob is added to the world. Regions in strict mode also
 * refuse every other way an unlisted entity can get in (spawn eggs, commands, other mods).
 */
public final class MobWhitelistEvents {

    private MobWhitelistEvents() {
    }

    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        String dimension = event.getLevel().getLevel().dimension().location().toString();
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();

        if (!Minspawn8Config.isMobAllowedAt(dimension, pos, mobId)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }
        Entity entity = event.getEntity();
        if (isNeverBlocked(entity)) {
            return;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        // Vanilla non-mobs (armor stands, minecarts, ...) are not "mobs"; modded entities of any kind are
        // treated as mobs because that is how things like corpses show up.
        if (!(entity instanceof Mob) && key.getNamespace().equals("minecraft")) {
            return;
        }
        String dimension = event.getLevel().dimension().location().toString();
        if (Minspawn8Config.isBlockedByStrictRegion(dimension, entity.blockPosition(), key.toString())) {
            event.setCanceled(true);
        }
    }

    private static boolean isNeverBlocked(Entity entity) {
        return entity instanceof Player
                || entity instanceof ItemEntity
                || entity instanceof ExperienceOrb
                || entity instanceof Projectile
                || entity instanceof VehicleEntity
                || entity instanceof HangingEntity
                || entity instanceof LightningBolt
                || entity instanceof FallingBlockEntity
                || entity instanceof PrimedTnt
                || entity instanceof AreaEffectCloud;
    }
}
