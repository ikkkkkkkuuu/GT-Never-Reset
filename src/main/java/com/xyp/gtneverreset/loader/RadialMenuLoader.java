package com.xyp.gtneverreset.loader;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.client.action.radialmenu.ActionExecutors;
import com.xyp.gtneverreset.client.action.radialmenu.CommandActionExecutor;
import com.xyp.gtneverreset.client.action.radialmenu.KeyActionExecutor;
import com.xyp.gtneverreset.client.action.radialmenu.ProfileSwitchExecutor;
import com.xyp.gtneverreset.client.action.radialmenu.ScriptActionExecutor;
import com.xyp.gtneverreset.client.action.radialmenu.ScriptResumeExecutor;
import com.xyp.gtneverreset.client.action.radialmenu.SequenceExecutor;
import com.xyp.gtneverreset.client.action.radialmenu.ToolBeltMenu;
import com.xyp.gtneverreset.client.command.CommandRadialMenu;
import com.xyp.gtneverreset.client.event.RadialMenuInputHandler;
import com.xyp.gtneverreset.client.keybind.WheelKeyBindings;
import com.xyp.gtneverreset.client.profile.radialmenu.ProfileManager;
import com.xyp.gtneverreset.client.profile.radialmenu.ProfileStorage;
import com.xyp.gtneverreset.client.render.radialmenu.IconResourceReloadHandler;
import com.xyp.gtneverreset.client.script.radialmenu.ChatCapture;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionTypes;

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
