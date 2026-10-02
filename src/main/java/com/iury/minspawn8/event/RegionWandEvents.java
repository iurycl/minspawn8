package com.iury.minspawn8.event;

import com.iury.minspawn8.config.Minspawn8Config;
import com.iury.minspawn8.item.Minspawn8Items;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Flan-style two-click region selection: right-click a block with the Spawn Selection Feather
 * to set corner 1, right-click another block to set corner 2 and create the region.
 */
public final class RegionWandEvents {

    private record Selection(ResourceLocation dimension, BlockPos pos) {
    }

    private static final Map<UUID, Selection> PENDING_SELECTIONS = new HashMap<>();

    private RegionWandEvents() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!(player instanceof ServerPlayer serverPlayer) || stack.isEmpty()
                || !stack.is(Minspawn8Items.SPAWN_FEATHER.get())) {
            return;
        }

        event.setCanceled(true);

        ServerLevel level = (ServerLevel) event.getLevel();
        ResourceLocation dimension = level.dimension().location();
        BlockPos pos = event.getPos();
        UUID playerId = serverPlayer.getUUID();

        Selection pending = PENDING_SELECTIONS.get(playerId);

        if (pending == null || !pending.dimension().equals(dimension)) {
            PENDING_SELECTIONS.put(playerId, new Selection(dimension, pos));
            serverPlayer.sendSystemMessage(Component.literal(
                    "[MinSpawn8] Ponto 1 definido em " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                            + ". Clique com o direito em outro bloco para definir o ponto 2."));
            return;
        }

        PENDING_SELECTIONS.remove(playerId);
        Minspawn8Config.Region region = Minspawn8Config.createRegion(dimension, pending.pos(), pos);
        serverPlayer.sendSystemMessage(Component.literal(
                "[MinSpawn8] Regiao #" + region.id + " criada (" + pending.pos().toShortString()
                        + " -> " + pos.toShortString() + "). Use /minspawn8 region percent " + region.id
                        + " hostile|nonhostile <percentual> para ajustar a taxa de spawn."));
    }
}
