package com.xyp.gtneverreset.loader;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.client.command.CommandRadialMenu;
import com.xyp.gtneverreset.client.radialmenu.ActionTypes;
import com.xyp.gtneverreset.client.radialmenu.RadialMenuInputHandler;
import com.xyp.gtneverreset.client.radialmenu.action.ActionExecutors;
import com.xyp.gtneverreset.client.radialmenu.action.CommandActionExecutor;
import com.xyp.gtneverreset.client.radialmenu.action.KeyActionExecutor;
import com.xyp.gtneverreset.client.radialmenu.action.ProfileSwitchExecutor;
import com.xyp.gtneverreset.client.radialmenu.action.ScriptActionExecutor;
import com.xyp.gtneverreset.client.radialmenu.action.ScriptResumeExecutor;
import com.xyp.gtneverreset.client.radialmenu.action.SequenceExecutor;
import com.xyp.gtneverreset.client.radialmenu.action.ToolBeltMenu;
import com.xyp.gtneverreset.client.radialmenu.input.WheelKeyBindings;
import com.xyp.gtneverreset.client.radialmenu.profile.ProfileManager;
import com.xyp.gtneverreset.client.radialmenu.profile.ProfileStorage;
import com.xyp.gtneverreset.client.radialmenu.render.IconResourceReloadHandler;
import com.xyp.gtneverreset.client.radialmenu.script.ChatCapture;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RadialMenuLoader {

    public static void preInit() {
        ProfileStorage.init(Minecraft.getMinecraft().mcDataDir);
        ActionTypes.registerDefaults();
        ActionExecutors.register(new KeyActionExecutor());
        ActionExecutors.register(new ProfileSwitchExecutor());
        ActionExecutors.register(new CommandActionExecutor());
        ActionExecutors.register(new SequenceExecutor());
        ActionExecutors.register(new ScriptActionExecutor());
        ActionExecutors.register(new ScriptResumeExecutor());
        ActionExecutors.register(new ToolBeltMenu());
    }

    public static void init() {
        ProfileManager.load();
        ClientCommandHandler.instance.registerCommand(new CommandRadialMenu());
        IconResourceReloadHandler.register();
        RadialMenuInputHandler input = new RadialMenuInputHandler();
        MinecraftForge.EVENT_BUS.register(input);
        MinecraftForge.EVENT_BUS.register(new ChatCapture.Listener());
        WheelKeyBindings.register();
        FMLCommonHandler.instance()
            .bus()
            .register(input);
    }
}
