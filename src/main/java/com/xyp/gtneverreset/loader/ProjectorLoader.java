package com.xyp.gtneverreset.loader;

import java.io.File;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.client.projector.ProjectorInput;
import com.xyp.gtneverreset.common.command.ProjectorSimulationCommand;
import com.xyp.gtneverreset.common.projector.ProjectorSimulation;
import com.xyp.gtneverreset.common.projector.ProjectorSimulationWorld;
import com.xyp.gtneverreset.config.ToolsConfig;

import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;

public class ProjectorLoader implements IContentLoader {

    @Override
    public void init() {
        if (!ToolsConfig.enableMultiblockProjector) return;
        MinecraftForge.EVENT_BUS.register(new ProjectorSimulation.Visits());
        GregTechAPI.addDummyWorld(ProjectorSimulationWorld.class);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        if (!ToolsConfig.enableMultiblockProjector || !(boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment"))
            return;
        event.registerServerCommand(new ProjectorSimulationCommand());
        ProjectorSimulationCommand.scheduleProbe();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void clientPreInit(File configDirectory) {
        if (ToolsConfig.enableMultiblockProjector) ProjectorInput.register();
    }
}
