package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Paths;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "itemregistrationqa", name = "Item Registration QA", version = "1", dependencies = "after:gtneverreset")
public final class ItemRegistrationChecks {

    private boolean done;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END || Minecraft.getMinecraft().currentScreen == null) return;
        done = true;
        try {
            String[] ids = { "PermanentMagnetRing", "HarvestRing", "packaged_wireless_connector", "basic_packaged_core",
                "assembly_line_packaged_core", "advanced_assembly_line_packaged_core",
                "circuit_assembly_line_packaged_core", "StableEndCrystal", "wireless_dual_interface_terminal",
                "wireless_frequency_card", "wireless_power_card", "WildcardPattern", "advanced_io_bus",
                "stock_io_interface_part", "universal_wand", "item.VeinMiningPickaxe" };
            for (String id : ids) {
                if (cpw.mods.fml.common.registry.GameRegistry.findItem("gtneverreset", id) == null) {
                    throw new AssertionError("missing existing registration: " + id);
                }
            }
            var original = com.xyp.gtneverreset.loader.ItemLoader.wirelessConnector;
            new com.xyp.gtneverreset.common.item.items.ItemWirelessConnector();
            new com.xyp.gtneverreset.common.item.items.ItemPackagedCore(false);
            new com.xyp.gtneverreset.common.item.items.ItemAdvancedIOBus();
            if (cpw.mods.fml.common.registry.GameRegistry.findItem("gtneverreset", "packaged_wireless_connector")
                != original
                || com.xyp.gtneverreset.utils.enums.GTNRItemList.ItemWirelessConnector.getItem() != original) {
                throw new AssertionError("construction modified global item registration");
            }
            var unregistered = new com.xyp.gtneverreset.common.item.items.ItemWirelessDualInterfaceTerminal();
            var wireless = appeng.api.AEApi.instance()
                .registries()
                .wireless();
            if (wireless.getWirelessTerminalHandler(new net.minecraft.item.ItemStack(unregistered)) != null
                || wireless.getWirelessTerminalHandler(
                    new net.minecraft.item.ItemStack(
                        com.xyp.gtneverreset.loader.ItemLoader.wirelessDualInterfaceTerminal))
                    != com.xyp.gtneverreset.loader.ItemLoader.wirelessDualInterfaceTerminal) {
                throw new AssertionError("wireless handler registration is not owned by the loader");
            }
            Files.writeString(Paths.get("result.txt"), "PASS");
            System.out
                .println("ITEM_REGISTRATION_QA PASS: preserved registry IDs and side-effect-free item construction");
            FMLCommonHandler.instance()
                .exitJava(0, false);
        } catch (Throwable failure) {
            failure.printStackTrace();
            FMLCommonHandler.instance()
                .exitJava(1, false);
        }
    }
}
