package com.iury.minspawn8;

import com.iury.minspawn8.command.Minspawn8Commands;
import com.iury.minspawn8.event.RegionWandEvents;
import com.iury.minspawn8.item.Minspawn8Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(Minspawn8Mod.MODID)
public class Minspawn8Mod {

    public static final String MODID = "minspawn8";

    public Minspawn8Mod(IEventBus modEventBus, ModContainer modContainer) {
        Minspawn8Items.ITEMS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, Minspawn8Commands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, RegionWandEvents::onRightClickBlock);
    }
}
