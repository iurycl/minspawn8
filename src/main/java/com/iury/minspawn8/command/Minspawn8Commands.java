package com.iury.minspawn8.command;

import com.iury.minspawn8.config.Minspawn8Config;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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

        dispatcher.register(Commands.literal("minspawn8")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> showHelp(ctx.getSource()))
                .then(Commands.literal("help")
                        .executes(ctx -> showHelp(ctx.getSource())))
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
                                                IntegerArgumentType.getInteger(ctx, "id")))))));
    }

    private static int showHelp(CommandSourceStack source) {
        String[] lines = {
                "[MinSpawn8] Comandos disponiveis:",
                "/minspawn8 help - mostra esta lista de comandos",
                "/minspawn8 distance - mostra a distancia minima de spawn atual (hostil e nao-hostil)",
                "/minspawn8 distance hostile <blocos> - define a distancia minima para mobs hostis (0-128)",
                "/minspawn8 distance nonhostile <blocos> - define a distancia minima para mobs nao-hostis (0-128)",
                "/minspawn8 region list - lista todas as regioes marcadas",
                "/minspawn8 region percent <id> - mostra os percentuais atuais de uma regiao",
                "/minspawn8 region percent <id> hostile <percentual> - define a taxa de spawn da regiao para hostis (100 = vanilla)",
                "/minspawn8 region percent <id> nonhostile <percentual> - define a taxa de spawn da regiao para nao-hostis (100 = vanilla)",
                "/minspawn8 region remove <id> - remove uma regiao"
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
                            + "% nao-hostil=" + region.percentNonHostile + "%"), false);
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
