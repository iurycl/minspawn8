package com.iury.minspawn8.util;

/**
 * NaturalSpawner.spawnForChunk calls spawnCategoryForChunk once per MobCategory, synchronously,
 * on the server thread. NaturalSpawnerRegionMixin sets this flag right before each call so that
 * NaturalSpawnerMixin (which patches a private method a few calls deeper, where the MobCategory
 * is no longer directly in scope) can tell whether the current spawn attempt is for a hostile or
 * non-hostile category.
 * <p>
 * Deliberately NOT in the com.iury.minspawn8.mixin package: Sponge Mixin reserves that whole
 * package for mixin classes themselves and refuses to class-load anything else from it at runtime.
 */
public final class SpawnCategoryContext {

    private static final ThreadLocal<Boolean> HOSTILE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private SpawnCategoryContext() {
    }

    public static void setHostile(boolean hostile) {
        HOSTILE.set(hostile);
    }

    public static boolean isHostile() {
        return HOSTILE.get();
    }
}
