package com.iury.minspawn8.item;

import com.iury.minspawn8.Minspawn8Mod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Minspawn8Items {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Minspawn8Mod.MODID);

    public static final DeferredItem<Item> SPAWN_FEATHER = ITEMS.registerItem(
            "spawn_feather",
            props -> new Item(props.stacksTo(1).rarity(Rarity.UNCOMMON))
    );

    private Minspawn8Items() {
    }
}
