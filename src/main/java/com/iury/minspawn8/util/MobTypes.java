package com.iury.minspawn8.util;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/** Decides which entity types can go on a region's mob list. */
public final class MobTypes {

    private MobTypes() {
    }

    /**
     * True for living creatures (zombies, cows, but also iron golems, villagers and snow golems, which
     * the game files under MobCategory.MISC). Items, projectiles, vehicles, markers and the like have
     * no default attributes and are left out, as are armor stands and players.
     */
    public static boolean isListable(EntityType<?> type) {
        if (!type.canSummon() || type == EntityType.ARMOR_STAND || type == EntityType.PLAYER) {
            return false;
        }
        return DefaultAttributes.hasSupplier(type);
    }
}
