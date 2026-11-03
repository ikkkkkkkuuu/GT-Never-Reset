package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.entity.EntityUselessWandItem;
import com.xyp.gtneverreset.common.entity.StableEndCrystalEntity;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.EntityRegistry;

public final class EntityLoader implements IContentLoader {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        registerEntities();
    }

    public static void registerEntities() {
        EntityRegistry.registerModEntity(
            EntityUselessWandItem.class,
            "useless_wand_item",
            1,
            GTNeverReset.INSTANCE,
            64,
            20,
            true);
        // #tr entity.gtneverreset.StableEndCrystal.name
        // # Stable End Crystal
        // # zh_CN 稳定末影水晶
        EntityRegistry.registerModEntity(
            StableEndCrystalEntity.class,
            "StableEndCrystal",
            0,
            GTNeverReset.INSTANCE,
            64,
            20,
            false);
    }
}
