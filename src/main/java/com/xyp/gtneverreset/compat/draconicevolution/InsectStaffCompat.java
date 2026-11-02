package com.xyp.gtneverreset.compat.draconicevolution;

import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.client.render.InsectStaffRenderer;
import com.xyp.gtneverreset.common.item.items.InsectStaff;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class InsectStaffCompat {

    private InsectStaffCompat() {}

    public static void register() {
        InsectStaff staff = new InsectStaff();
        MinecraftForge.EVENT_BUS.register(staff);
        FMLCommonHandler.instance()
            .bus()
            .register(staff);
    }

    @SideOnly(Side.CLIENT)
    public static void registerRenderer() {
        MinecraftForgeClient.registerItemRenderer(GTNRItemList.InsectStaff.getItem(), new InsectStaffRenderer());
    }
}
