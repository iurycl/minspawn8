package com.iury.minspawn8.command;

import com.iury.minspawn8.config.Minspawn8Config;
import com.iury.minspawn8.gui.Minspawn8Menu;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/**
 * /minspawn8 [help]
 * /minspawn8 distance [hostile|nonhostile] [blocks]
 * /minspawn8 region list
 * /minspawn8 region percent <id> <hostile|nonhostile> <percent>
 * /minspawn8 region remove <id>
 */
public final class Minspawn8Commands {

    private Minspawn8Commands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandBuildContext buildContext = event.getBuildContext();

        dispatcher.register(Commands.literal("minspawn8")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> showHelp(ctx.getSource()))
                .then(Commands.literal("help")
                        .executes(ctx -> showHelp(ctx.getSource())))
                .then(Commands.literal("gui")
                        .executes(ctx -> {
                            Minspawn8Menu.open(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("distance")
                        .executes(ctx -> showDistance(ctx.getSource()))
                        .then(Commands.literal("hostile")
                                .executes(ctx -> showDistance(ctx.getSource()))
                                .then(Commands.argument("blocks", IntegerArgumentType.integer(0, 128))
                                        .executes(ctx -> setDistance(ctx.getSource(), true,
                                                IntegerArgumentType.getInteger(ctx, "blocks")))))
                        .then(Commands.literal("nonhostile")
                                .executes(ctx -> showDistance(ctx.getSource()))
                                .then(Commands.argument("blocks", IntegerArgumentType.integer(0, 128))
                                        .executes(ctx -> setDistance(ctx.getSource(), false,
                                                IntegerArgumentType.getInteger(ctx, "blocks"))))))
                .then(Commands.literal("region")
                        .then(Commands.literal("list")
                                .executes(ctx -> listRegions(ctx.getSource())))
                        .then(Commands.literal("percent")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(ctx -> showRegionPercent(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))
                                        .then(Commands.literal("hostile")
                                                .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0, 100000))
                                                        .executes(ctx -> setRegionPercent(ctx.getSource(), true,
                                                                IntegerArgumentType.getInteger(ctx, "id"),
                                                                DoubleArgumentType.getDouble(ctx, "percent")))))
                                        .then(Commands.literal("nonhostile")
                                                .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0, 100000))
                                                        .executes(ctx -> setRegionPercent(ctx.getSource(), false,
                                                                IntegerArgumentType.getInteger(ctx, "id"),
                                                                DoubleArgumentType.getDouble(ctx, "percent")))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(ctx -> removeRegion(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "id")))))
                        .then(Commands.literal("strict")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(ctx -> showStrict(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> setStrict(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "id"),
                                                        BoolArgumentType.getBool(ctx, "enabled"))))))
                        .then(mobsNode(buildContext))));
    }

    private static int showStrict(CommandSourceStack source, int id) {
        for (Minspawn8Config.Region region : Minspawn8Config.getRegions()) {
            if (region.id == id) {
                source.sendSuccess(() -> Component.literal(
                        "[MinSpawn8] Regiao #" + id + ": modo estrito " + (region.strict ? "LIGADO" : "desligado")), false);
                return 1;
            }
        }
        source.sendFailure(Component.literal("[MinSpawn8] Regiao #" + id + " nao encontrada"));
        return 0;
    }

    private static int setStrict(CommandSourceStack source, int id, boolean enabled) {
        if (!Minspawn8Config.setRegionStrict(id, enabled)) {
            source.sendFailure(Component.literal("[MinSpawn8] Regiao #" + id + " nao encontrada"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "[MinSpawn8] Regiao #" + id + ": modo estrito " + (enabled ? "LIGADO" : "desligado")), true);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> mobsNode(CommandBuildContext buildContext) {
        return Commands.literal("mobs")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .executes(ctx -> listMobs(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))
                        .then(Commands.literal("list")
                                .executes(ctx -> listMobs(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"))))
                        .then(Commands.literal("clear")
                                .executes(ctx -> clearMobs(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("mob", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .executes(ctx -> addMob(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "id"),
                                                ResourceArgument.getSummonableEntityType(ctx, "mob")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("mob", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .executes(ctx -> removeMob(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "id"),
                                                ResourceArgument.getEntityType(ctx, "mob")
                                                        .key().location().toString())))));
    }

    private static int listMobs(CommandSourceStack source, int id) {
        List<String> mobs = Minspawn8Config.getAllowedMobs(id);
        if (mobs == null) {
            source.sendFailure(Component.literal("[MinSpawn8] Regiao #" + id + " nao encontrada"));
            return 0;
        }
        if (mobs.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "[MinSpawn8] Regiao #" + id + ": lista vazia (todos os mobs podem nascer)"), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "[MinSpawn8] Regiao #" + id + ": so nascem " + mobs.size() + " mob(s):"), false);
            for (String mob : mobs) {
                source.sendSuccess(() -> Component.literal(" - " + mob), false);
            }
        }
        return 1;
    }

    private static int addMob(CommandSourceStack source, int id, Holder.Reference<EntityType<?>> mob) {
        String mobId = mob.key().location().toString();
        if (mob.value().getCategory() == MobCategory.MISC) {
            source.sendFailure(Component.literal("[MinSpawn8] " + mobId + " nao e um mob que nasce naturalmente"));
            return 0;
        }
        if (!Minspawn8Config.addAllowedMob(id, mobId)) {
            source.sendFailure(Component.literal("[MinSpawn8] Regiao #" + id + " nao encontrada"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "[MinSpawn8] Regiao #" + id + ": " + mobId + " adicionado a lista"), true);
        return 1;
    }

    private static int removeMob(CommandSourceStack source, int id, String mobId) {
        int result = Minspawn8Config.removeAllowedMob(id, mobId);
        if (result < 0) {
            source.sendFailure(Component.literal("[MinSpawn8] Regiao #" + id + " nao encontrada"));
            return 0;
        }
        if (result == 0) {
            source.sendFailure(Component.literal("[MinSpawn8] " + mobId + " nao esta na lista da regiao #" + id));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "[MinSpawn8] Regiao #" + id + ": " + mobId + " removido da lista"), true);
        return 1;
    }

    private static int clearMobs(CommandSourceStack source, int id) {
        if (!Minspawn8Config.clearAllowedMobs(id)) {
            source.sendFailure(Component.literal("[MinSpawn8] Regiao #" + id + " nao encontrada"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "[MinSpawn8] Regiao #" + id + ": lista limpa (todos os mobs podem nascer)"), true);
        return 1;
    }

    private static int showHelp(CommandSourceStack source) {
        String[] lines = {
                "[MinSpawn8] Comandos disponiveis:",
                "/minspawn8 help - mostra esta lista de comandos",
                "/minspawn8 gui - abre o menu (baú) para editar regioes, percentuais, distancias e mobs. Tambem: shift + botao direito com a pena",
                "/minspawn8 distance - mostra a distancia minima de spawn atual (hostil e nao-hostil)",
                "/minspawn8 distance hostile <blocos> - define a distancia minima para mobs hostis (0-128)",
                "/minspawn8 distance nonhostile <blocos> - define a distancia minima para mobs nao-hostis (0-128)",
                "/minspawn8 region list - lista todas as regioes marcadas",
                "/minspawn8 region percent <id> - mostra os percentuais atuais de uma regiao",
                "/minspawn8 region percent <id> hostile <percentual> - define a taxa de spawn da regiao para hostis (100 = vanilla)",
                "/minspawn8 region percent <id> nonhostile <percentual> - define a taxa de spawn da regiao para nao-hostis (100 = vanilla)",
                "/minspawn8 region remove <id> - remove uma regiao",
                "/minspawn8 region mobs <id> [list] - mostra os mobs permitidos na regiao (lista vazia = todos)",
                "/minspawn8 region mobs <id> add <mob> - permite so esse mob (e os outros da lista) nascer na regiao",
                "/minspawn8 region mobs <id> remove <mob> - tira um mob da lista da regiao",
                "/minspawn8 region mobs <id> clear - limpa a lista (todos os mobs voltam a nascer)",
                "/minspawn8 region strict <id> [true|false] - modo estrito: alem do spawn natural, bloqueia qualquer mob/entidade fora da lista (ovos, comandos, outros mods, corpses). Lista vazia + estrito = nenhum mob"
        };
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int showDistance(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "[MinSpawn8] Distancia minima de spawn -> hostil: " + Minspawn8Config.getMinSpawnDistanceHostile()
                        + " blocos | nao-hostil: " + Minspawn8Config.getMinSpawnDistanceNonHostile() + " blocos"), false);
        return 1;
    }

    private static int setDistance(CommandSourceStack source, boolean hostile, int blocks) {
        if (hostile) {
            Minspawn8Config.setMinSpawnDistanceHostile(blocks);
        } else {
            Minspawn8Config.setMinSpawnDistanceNonHostile(blocks);
        }
        source.sendSuccess(() -> Component.literal(
                "[MinSpawn8] Distancia minima de spawn (" + (hostile ? "hostil" : "nao-hostil")
                        + ") definida para " + blocks + " blocos"), true);
        return 1;
    }

    private static int listRegions(CommandSourceStack source) {
        List<Minspawn8Config.Region> regions = Minspawn8Config.getRegions();
        if (regions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("[MinSpawn8] Nenhuma regiao configurada."), false);
            return 1;
        }
        for (Minspawn8Config.Region region : regions) {
            source.sendSuccess(() -> Component.literal(
                    "[MinSpawn8] #" + region.id + " dim=" + region.dimension
                            + " (" + region.x1 + "," + region.y1 + "," + region.z1 + ") -> ("
                            + region.x2 + "," + region.y2 + "," + region.z2 + ") hostil=" + region.percentHostile
                            + "% nao-hostil=" + region.percentNonHostile + "% mobs="
                            + (region.allowedMobs.isEmpty() ? "todos" : String.join(",", region.allowedMobs))
                            + (region.strict ? " ESTRITO" : "")), false);
        }
        return 1;
    }

    private static int showRegionPercent(CommandSourceStack source, int id) {
        for (Minspawn8Config.Region region : Minspawn8Config.getRegions()) {
            if (region.id == id) {
                source.sendSuccess(() -> Component.literal(
                        "[MinSpawn8] Regiao #" + id + " -> hostil: " + region.percentHostile
                                + "% | nao-hostil: " + region.percentNonHostile + "%"), false);
                return 1;
            }
        }
        source.sendFailure(Component.literal("[MinSpawn8] Regiao #" + id + " nao encontrada"));
        return 0;
    }

    private static int setRegionPercent(CommandSourceStack source, boolean hostile, int id, double percent) {
        boolean ok = hostile
                ? Minspawn8Config.setRegionPercentHostile(id, percent)
                : Minspawn8Config.setRegionPercentNonHostile(id, percent);
        if (ok) {
            source.sendSuccess(() -> Component.literal(
                    "[MinSpawn8] Regiao #" + id + " (" + (hostile ? "hostil" : "nao-hostil")
                            + ") agora com " + percent + "% de taxa de spawn"), true);
            return 1;
        }
        source.sendFailure(Component.literal("[MinSpawn8] Regiao #" + id + " nao encontrada"));
        return 0;
    }

    private static int removeRegion(CommandSourceStack source, int id) {
        if (Minspawn8Config.removeRegion(id)) {
            source.sendSuccess(() -> Component.literal("[MinSpawn8] Regiao #" + id + " removida"), true);
            return 1;
        }
        source.sendFailure(Component.literal("[MinSpawn8] Regiao #" + id + " nao encontrada"));
        return 0;
    }
}
