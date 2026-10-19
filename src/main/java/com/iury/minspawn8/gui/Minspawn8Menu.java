package com.iury.minspawn8.gui;

import com.iury.minspawn8.config.Minspawn8Config;
import com.iury.minspawn8.util.MobTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chest-style, server-driven admin menu. It is a vanilla 9x6 chest screen whose slots are buttons:
 * the client needs no custom screen code. A single menu instance switches between views so the
 * mouse stays put between clicks.
 */
public final class Minspawn8Menu extends ChestMenu {

    private static final int SLOTS = 54;
    private static final int PAGE_SIZE = 45;

    private enum View { REGIONS, REGION, MOBS, PICKER, DISTANCE }

    private enum Filter {
        ALL("Todos"), HOSTILE("Hostis"), ANIMALS("Animais"), WATER("Aquáticos"), AMBIENT("Ambiente"),
        OTHER("Golems e outros");

        final String label;

        Filter(String label) {
            this.label = label;
        }

        boolean matches(MobCategory c) {
            return switch (this) {
                case ALL -> true;
                case HOSTILE -> c == MobCategory.MONSTER;
                case ANIMALS -> c == MobCategory.CREATURE || c == MobCategory.AXOLOTLS;
                case WATER -> c == MobCategory.WATER_CREATURE || c == MobCategory.UNDERGROUND_WATER_CREATURE
                        || c == MobCategory.WATER_AMBIENT;
                case AMBIENT -> c == MobCategory.AMBIENT;
                case OTHER -> c == MobCategory.MISC;
            };
        }

        Filter next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private final Container container;
    private final Map<Integer, Runnable> actions = new HashMap<>();
    private View view = View.REGIONS;
    private int page;
    private int regionId;
    private Filter filter = Filter.ALL;
    private boolean confirmDelete;

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new Minspawn8Menu(id, inv, new SimpleContainer(SLOTS)),
                Component.literal("MinSpawn8")));
    }

    private Minspawn8Menu(int containerId, Inventory inventory, SimpleContainer container) {
        super(MenuType.GENERIC_9x6, containerId, inventory, container, 6);
        this.container = container;
        render();
    }

    // ------------------------------------------------------------------ click handling

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!serverPlayer.hasPermissions(2)) {
            serverPlayer.closeContainer();
            return;
        }
        if (slot >= 0 && slot < SLOTS && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE)) {
            Runnable action = actions.get(slot);
            if (action != null) {
                action.run();
                render();
            }
        }
        // Never let the client move items: resend the real state every time.
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void go(View target) {
        view = target;
        page = 0;
        confirmDelete = false;
    }

    // ------------------------------------------------------------------ rendering

    private void render() {
        actions.clear();
        container.clearContent();
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
        for (int i = 0; i < SLOTS; i++) {
            container.setItem(i, filler.copy());
        }
        switch (view) {
            case REGIONS -> renderRegions();
            case REGION -> renderRegion();
            case MOBS -> renderMobs();
            case PICKER -> renderPicker();
            case DISTANCE -> renderDistance();
        }
    }

    private void renderRegions() {
        List<Minspawn8Config.Region> regions = Minspawn8Config.getRegions();
        int maxPage = Math.max(0, (regions.size() - 1) / PAGE_SIZE);
        page = Math.min(page, maxPage);

        if (regions.isEmpty()) {
            set(22, icon(Items.BARRIER, "Nenhuma região ainda", ChatFormatting.RED,
                    "Use a Pena de Seleção:", "clique direito em 2 blocos", "para criar uma região."), null);
        }
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            if (index >= regions.size()) {
                break;
            }
            Minspawn8Config.Region r = regions.get(index);
            int id = r.id;
            List<String> mobs = r.allowedMobs;
            set(i, icon(dimensionIcon(r.dimension), "Região #" + r.id, ChatFormatting.YELLOW,
                    r.dimension,
                    "De " + r.x1 + ", " + r.y1 + ", " + r.z1,
                    "até " + r.x2 + ", " + r.y2 + ", " + r.z2,
                    "Hostis: " + fmt(r.percentHostile) + "%  |  Não-hostis: " + fmt(r.percentNonHostile) + "%",
                    "Misc (golem de ferro): " + fmt(r.percentMisc) + "%",
                    "Mobs: " + (mobs.isEmpty() ? "todos" : mobs.size() + " na lista"),
                    "",
                    "Clique para editar"), () -> {
                regionId = id;
                go(View.REGION);
            });
        }
        pager(45, 53, maxPage);
        set(49, icon(Items.COMPASS, "Distância mínima de spawn", ChatFormatting.AQUA,
                "Hostis: " + Minspawn8Config.getMinSpawnDistanceHostile() + " blocos",
                "Não-hostis: " + Minspawn8Config.getMinSpawnDistanceNonHostile() + " blocos",
                "", "Clique para editar"), () -> go(View.DISTANCE));
        set(47, icon(Items.BOOK, "Ajuda", ChatFormatting.GREEN,
                "Clique em uma região para editar", "os percentuais de spawn e a", "lista de mobs permitidos."), null);
    }

    private void renderRegion() {
        Minspawn8Config.Region r = findRegion(regionId);
        if (r == null) {
            go(View.REGIONS);
            renderRegions();
            return;
        }
        set(4, icon(dimensionIcon(r.dimension), "Região #" + r.id, ChatFormatting.YELLOW,
                r.dimension,
                "De " + r.x1 + ", " + r.y1 + ", " + r.z1,
                "até " + r.x2 + ", " + r.y2 + ", " + r.z2), null);

        percentRow(10, Minspawn8Config.Bucket.HOSTILE, r.percentHostile, "Hostis", Items.ZOMBIE_HEAD);
        percentRow(19, Minspawn8Config.Bucket.NON_HOSTILE, r.percentNonHostile, "Não-hostis", Items.COW_SPAWN_EGG);
        percentRow(28, Minspawn8Config.Bucket.MISC, r.percentMisc, "Misc (golem de ferro)", Items.IRON_BLOCK);

        List<String> mobs = r.allowedMobs;
        set(40, icon(Items.ZOMBIE_SPAWN_EGG, "Mobs permitidos", ChatFormatting.GOLD,
                mobs.isEmpty() ? "Todos podem nascer" : "Só nascem " + mobs.size() + " mob(s)",
                "", "Clique para editar a lista"), () -> go(View.MOBS));

        int id = r.id;
        boolean strict = r.strict;
        set(42, icon(strict ? Items.REDSTONE_TORCH : Items.LEVER,
                "Modo estrito: " + (strict ? "LIGADO" : "desligado"),
                strict ? ChatFormatting.RED : ChatFormatting.GRAY,
                "Ligado: bloqueia qualquer mob fora da lista,",
                "não só o spawn natural (ovos, comandos,",
                "outros mods, corpses).",
                "Lista vazia + estrito = nenhum mob.",
                "", "Clique para alternar"), () -> Minspawn8Config.setRegionStrict(id, !strict));

        set(45, icon(Items.ARROW, "Voltar", ChatFormatting.WHITE), () -> go(View.REGIONS));
        if (confirmDelete) {
            set(53, icon(Items.TNT, "Clique de novo para APAGAR", ChatFormatting.DARK_RED,
                    "Isso remove a região #" + r.id + "."), () -> {
                Minspawn8Config.removeRegion(r.id);
                go(View.REGIONS);
            });
        } else {
            set(53, icon(Items.BARRIER, "Apagar região", ChatFormatting.RED,
                    "Pede confirmação"), () -> confirmDelete = true);
        }
    }

    private void percentRow(int firstSlot, Minspawn8Config.Bucket bucket, double value, String label, Item centerIcon) {
        int id = regionId;
        boolean misc = bucket == Minspawn8Config.Bucket.MISC;
        int small = misc ? 10 : 1;
        int big = misc ? 100 : 10;
        set(firstSlot, button(Items.RED_STAINED_GLASS_PANE, "-" + big + "%"), () -> adjustPercent(id, bucket, -big));
        set(firstSlot + 1, button(Items.RED_STAINED_GLASS_PANE, "-" + small + "%"), () -> adjustPercent(id, bucket, -small));
        set(firstSlot + 3, icon(centerIcon, label + ": " + fmt(value) + "%", ChatFormatting.WHITE,
                misc ? "100% = produção normal dos aldeões" : "100% = spawn normal do jogo",
                misc ? "300% = 3 golems por vez, 0% = nenhum" : "Acima de 100% aumenta, abaixo reduz",
                "A zona ao redor recebe o efeito espelhado",
                "", "Clique aqui para voltar a 100%"), () -> setPercent(id, bucket, 100));
        set(firstSlot + 5, button(Items.LIME_STAINED_GLASS_PANE, "+" + small + "%"), () -> adjustPercent(id, bucket, small));
        set(firstSlot + 6, button(Items.LIME_STAINED_GLASS_PANE, "+" + big + "%"), () -> adjustPercent(id, bucket, big));
    }

    private void renderMobs() {
        List<String> allowed = Minspawn8Config.getAllowedMobs(regionId);
        if (allowed == null) {
            go(View.REGIONS);
            renderRegions();
            return;
        }
        int maxPage = Math.max(0, (allowed.size() - 1) / PAGE_SIZE);
        page = Math.min(page, maxPage);

        if (allowed.isEmpty()) {
            set(22, icon(Items.BARRIER, "Lista vazia", ChatFormatting.RED,
                    "Todos os mobs podem nascer nesta região.", "Use \"Adicionar mob\" para restringir."), null);
        }
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            if (index >= allowed.size()) {
                break;
            }
            String mobId = allowed.get(index);
            int id = regionId;
            set(i, mobIcon(mobId, false), () -> Minspawn8Config.removeAllowedMob(id, mobId));
        }
        int id = regionId;
        set(45, icon(Items.ARROW, "Voltar", ChatFormatting.WHITE), () -> go(View.REGION));
        set(46, icon(Items.SPECTRAL_ARROW, "Página anterior", ChatFormatting.WHITE), () -> page = Math.max(0, page - 1));
        set(47, icon(Items.LAVA_BUCKET, "Limpar lista", ChatFormatting.RED,
                "Remove todos e libera todos os mobs"), () -> Minspawn8Config.clearAllowedMobs(id));
        set(49, icon(Items.LIME_DYE, "Adicionar mob", ChatFormatting.GREEN,
                "Abre a lista de todos os mobs"), () -> go(View.PICKER));
        set(52, icon(Items.SPECTRAL_ARROW, "Próxima página", ChatFormatting.WHITE), () -> page = Math.min(maxPage, page + 1));
    }

    private void renderPicker() {
        List<String> allowed = Minspawn8Config.getAllowedMobs(regionId);
        if (allowed == null) {
            go(View.REGIONS);
            renderRegions();
            return;
        }
        List<EntityType<?>> types = mobTypes(filter);
        int maxPage = Math.max(0, (types.size() - 1) / PAGE_SIZE);
        page = Math.min(page, maxPage);
        int id = regionId;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            if (index >= types.size()) {
                break;
            }
            String mobId = key(types.get(index)).toString();
            boolean selected = allowed.contains(mobId);
            set(i, mobIcon(mobId, true, selected), () -> {
                if (selected) {
                    Minspawn8Config.removeAllowedMob(id, mobId);
                } else {
                    Minspawn8Config.addAllowedMob(id, mobId);
                }
            });
        }
        set(45, icon(Items.ARROW, "Voltar à lista", ChatFormatting.WHITE), () -> go(View.MOBS));
        set(46, icon(Items.SPECTRAL_ARROW, "Página anterior", ChatFormatting.WHITE), () -> page = Math.max(0, page - 1));
        set(49, icon(Items.HOPPER, "Filtro: " + filter.label, ChatFormatting.AQUA,
                types.size() + " mob(s) neste filtro", "", "Clique para trocar o filtro"), () -> {
            filter = filter.next();
            page = 0;
        });
        set(52, icon(Items.SPECTRAL_ARROW, "Próxima página", ChatFormatting.WHITE), () -> page = Math.min(maxPage, page + 1));
        set(53, icon(Items.BOOK, "Página " + (page + 1) + " / " + (maxPage + 1), ChatFormatting.GRAY,
                allowed.size() + " mob(s) selecionado(s)", "Brilho = está na lista"), null);
    }

    private void renderDistance() {
        distanceRow(10, true, Minspawn8Config.getMinSpawnDistanceHostile(), "Hostis", Items.ZOMBIE_HEAD);
        distanceRow(19, false, Minspawn8Config.getMinSpawnDistanceNonHostile(), "Não-hostis", Items.COW_SPAWN_EGG);
        set(45, icon(Items.ARROW, "Voltar", ChatFormatting.WHITE), () -> go(View.REGIONS));
    }

    private void distanceRow(int firstSlot, boolean hostile, int value, String label, Item centerIcon) {
        set(firstSlot, button(Items.RED_STAINED_GLASS_PANE, "-8 blocos"), () -> adjustDistance(hostile, -8));
        set(firstSlot + 1, button(Items.RED_STAINED_GLASS_PANE, "-1 bloco"), () -> adjustDistance(hostile, -1));
        set(firstSlot + 3, icon(centerIcon, label + ": " + value + " blocos", ChatFormatting.WHITE,
                "Distância mínima do jogador para um mob nascer",
                "Padrão do jogo: 24 blocos", "",
                "Clique aqui para voltar a 8 (padrão do mod)"), () -> setDistance(hostile, 8));
        set(firstSlot + 5, button(Items.LIME_STAINED_GLASS_PANE, "+1 bloco"), () -> adjustDistance(hostile, 1));
        set(firstSlot + 6, button(Items.LIME_STAINED_GLASS_PANE, "+8 blocos"), () -> adjustDistance(hostile, 8));
    }

    private void pager(int prevSlot, int nextSlot, int maxPage) {
        if (page > 0) {
            set(prevSlot, icon(Items.SPECTRAL_ARROW, "Página anterior", ChatFormatting.WHITE), () -> page--);
        }
        if (page < maxPage) {
            set(nextSlot, icon(Items.SPECTRAL_ARROW, "Próxima página", ChatFormatting.WHITE), () -> page++);
        }
    }

    // ------------------------------------------------------------------ actions

    private static void adjustPercent(int id, Minspawn8Config.Bucket bucket, double delta) {
        Minspawn8Config.Region r = findRegion(id);
        if (r != null) {
            setPercent(id, bucket, r.percent(bucket) + delta);
        }
    }

    private static void setPercent(int id, Minspawn8Config.Bucket bucket, double value) {
        double clamped = Math.max(0, Math.min(100000, Math.round(value * 100.0) / 100.0));
        Minspawn8Config.setRegionPercent(id, bucket, clamped);
    }

    private static void adjustDistance(boolean hostile, int delta) {
        int current = hostile ? Minspawn8Config.getMinSpawnDistanceHostile() : Minspawn8Config.getMinSpawnDistanceNonHostile();
        setDistance(hostile, current + delta);
    }

    private static void setDistance(boolean hostile, int value) {
        int clamped = Math.max(0, Math.min(128, value));
        if (hostile) {
            Minspawn8Config.setMinSpawnDistanceHostile(clamped);
        } else {
            Minspawn8Config.setMinSpawnDistanceNonHostile(clamped);
        }
    }

    // ------------------------------------------------------------------ helpers

    private void set(int slot, ItemStack stack, Runnable action) {
        container.setItem(slot, stack);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    private static Minspawn8Config.Region findRegion(int id) {
        for (Minspawn8Config.Region r : Minspawn8Config.getRegions()) {
            if (r.id == id) {
                return r;
            }
        }
        return null;
    }

    private static ResourceLocation key(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    /**
     * Every living creature that can be listed, including the MISC ones (iron golem, villager, ...).
     * Mobs with a spawn egg are shown with it; the others get a name tag carrying the mob's name.
     */
    private static List<EntityType<?>> mobTypes(Filter filter) {
        List<EntityType<?>> out = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (MobTypes.isListable(type) && filter.matches(type.getCategory())) {
                out.add(type);
            }
        }
        out.sort(Comparator.comparing((EntityType<?> t) -> !"minecraft".equals(key(t).getNamespace()))
                .thenComparing(t -> key(t).toString()));
        return out;
    }

    private static ItemStack mobIcon(String mobId, boolean picker) {
        return mobIcon(mobId, picker, true);
    }

    private static ItemStack mobIcon(String mobId, boolean picker, boolean selected) {
        ResourceLocation rl = ResourceLocation.tryParse(mobId);
        EntityType<?> type = rl == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
        SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
        ItemStack stack = new ItemStack(egg != null ? egg : Items.NAME_TAG);

        Component name = type != null ? type.getDescription().copy() : Component.literal(mobId);
        stack.set(DataComponents.CUSTOM_NAME, name.copy().withStyle(
                Style.EMPTY.withItalic(false).withColor(selected ? ChatFormatting.GREEN : ChatFormatting.WHITE)));
        List<Component> lore = new ArrayList<>();
        lore.add(loreLine(mobId));
        if (egg == null) {
            lore.add(loreLine("(sem ovo de spawn)"));
        }
        lore.add(loreLine(""));
        if (picker) {
            lore.add(loreLine(selected ? "Na lista. Clique para remover" : "Clique para adicionar"));
        } else {
            lore.add(loreLine("Clique para remover da lista"));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (selected) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }

    private static ItemStack button(Item item, String name) {
        return icon(item, name, ChatFormatting.WHITE);
    }

    private static ItemStack icon(Item item, String name, ChatFormatting color, String... lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)
                .withStyle(Style.EMPTY.withItalic(false).withColor(color)));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(loreLine(line));
            }
            stack.set(DataComponents.LORE, new ItemLore(lines));
        }
        return stack;
    }

    private static Component loreLine(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY));
    }

    private static Item dimensionIcon(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> Items.GRASS_BLOCK;
            case "minecraft:the_nether" -> Items.NETHERRACK;
            case "minecraft:the_end" -> Items.END_STONE;
            default -> Items.STONE;
        };
    }

    private static String fmt(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
