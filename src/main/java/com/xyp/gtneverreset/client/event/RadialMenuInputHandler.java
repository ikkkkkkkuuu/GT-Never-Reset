package com.xyp.gtneverreset.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.xyp.gtneverreset.client.action.radialmenu.ActionExecutors;
import com.xyp.gtneverreset.client.action.radialmenu.WithoutScreen;
import com.xyp.gtneverreset.client.gui.RadialGuiStack;
import com.xyp.gtneverreset.client.gui.RadialMenuGui;
import com.xyp.gtneverreset.client.gui.RadialProfileManagerGui;
import com.xyp.gtneverreset.client.keybind.KeyInjector;
import com.xyp.gtneverreset.client.keybind.KeybindStateTracker;
import com.xyp.gtneverreset.client.keybind.WheelKeyBindings;
import com.xyp.gtneverreset.client.profile.radialmenu.ProfileManager;
import com.xyp.gtneverreset.client.script.radialmenu.ScriptHost;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialMenuInputHandler {

    private boolean wheelKeyDown;

    private boolean autoBindApplied;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc.theWorld == null || mc.thePlayer == null) {
            if (wheelKeyDown) {
                wheelKeyDown = false;
            }
            autoBindApplied = false;
            KeybindStateTracker.releaseAll();

            if (ScriptHost.hasRunning()) {
                ScriptHost.cancelAll("left the world");
            }
            return;
        }

        if (!autoBindApplied) {
            autoBindApplied = true;
            applyAutoBind(mc);
        }

        KeybindStateTracker.onClientTick();
        if (ActionExecutors.runPending() && WithoutScreen.wheelIsUp()) {
            WithoutScreen.armNextTick();
        }

        ScriptHost.onClientTick();

        RadialGuiStack.openRequested();

        if (WheelKeyBindings.nextProfile.isPressed()) {
            ProfileManager.cycle(1);
        }
        if (WheelKeyBindings.previousProfile.isPressed()) {
            ProfileManager.cycle(-1);
        }
        if (WheelKeyBindings.openEditor.isPressed()) {
            RadialGuiStack.requestOpen(new RadialProfileManagerGui());
        }

        boolean down = KeyInjector.isPhysicallyDown(WheelKeyBindings.openWheel);
        if (down != wheelKeyDown) {
            wheelKeyDown = down;
            if (down) {
                openWheel(mc);
            } else {
                releaseWheel(mc);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onTickFirst(TickEvent.ClientTickEvent event) {
        WithoutScreen.beginTick();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTickLast(TickEvent.ClientTickEvent event) {
        WithoutScreen.endTick(event.phase == TickEvent.Phase.START);
    }

    private void applyAutoBind(Minecraft mc) {
        String serverAddress = null;
        String worldName = null;

        if (mc.isSingleplayer()) {
            if (mc.getIntegratedServer() != null) {
                worldName = mc.getIntegratedServer()
                    .getFolderName();
            }
        } else {
            ServerData data = mc.func_147104_D();
            if (data != null) {
                serverAddress = data.serverIP;
            }
        }

        ProfileManager.applyAutoBind(serverAddress, worldName);
    }

    private void openWheel(Minecraft mc) {
        if (mc.currentScreen != null) {
            return;
        }
        mc.displayGuiScreen(new RadialMenuGui());
    }

    private void releaseWheel(Minecraft mc) {
        if (mc.currentScreen instanceof RadialMenuGui) {
            ((RadialMenuGui) mc.currentScreen).onWheelKeyReleased();
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }
        if (Minecraft.getMinecraft().currentScreen instanceof RadialMenuGui) {
            event.setCanceled(true);
        }
    }

}
