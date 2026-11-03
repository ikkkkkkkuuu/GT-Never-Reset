package com.xyp.gtneverreset.client.wand;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;
import com.xyp.gtneverreset.common.packet.UniversalWandNetwork;
import com.xyp.gtneverreset.common.wand.UniversalWandFeature;
import com.xyp.gtneverreset.common.wand.UniversalWandUtilityHandler;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client key entry point; the key can be rebound through the normal Minecraft controls screen. */
public final class UniversalWandKeyHandler {

    private static final KeyBinding OPEN_GUI = new KeyBinding(
        // #tr key.gtneverreset.universal_wand
        // # Open Universal Wand GUI
        // # zh_CN 打开无用之杖界面
        "key.gtneverreset.universal_wand",
        Keyboard.KEY_G,
        "key.categories.gtneverreset");
    private static final KeyBinding FORCE_BEDROCK = new KeyBinding(
        // #tr key.gtneverreset.universal_wand.force_bedrock
        // # Force bedrock mining
        // # zh_CN 强制挖掘基岩
        "key.gtneverreset.universal_wand.force_bedrock",
        Keyboard.KEY_R,
        "key.categories.gtneverreset");
    private static final KeyBinding CHAIN = new KeyBinding(
        // #tr key.gtneverreset.universal_wand.chain
        // # Chain mining (hold)
        // # zh_CN 连锁挖掘（按住）
        "key.gtneverreset.universal_wand.chain",
        Keyboard.KEY_GRAVE,
        "key.categories.gtneverreset");
    private boolean lastChainHeld;
    private int chainHeartbeat;
    private static final KeyBinding UNDO = new KeyBinding(
        // #tr key.gtneverreset.universal_wand.undo
        // # Building undo (hold and right-click)
        // # zh_CN 建筑撤回（按住并右键）
        "key.gtneverreset.universal_wand.undo",
        Keyboard.KEY_LCONTROL,
        "key.categories.gtneverreset");
    private boolean lastUndoHeld;
    private int undoHeartbeat;
    private static final KeyBinding FLIGHT_SPEED = new KeyBinding(
        // #tr key.gtneverreset.universal_wand.flight_speed
        // # Universal Wand Flight Speed (Shift to Decrease)
        // # zh_CN 无用之杖飞行速度（Shift 降低）
        "key.gtneverreset.universal_wand.flight_speed",
        Keyboard.KEY_V,
        "key.categories.gtneverreset");

    public static boolean isChainKeyHeld() {
        return UniversalWandFeature.CHAIN.allowed() && CHAIN.getIsKeyPressed();
    }

    public static boolean isUndoKeyHeld() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.currentScreen == null && minecraft.inGameHasFocus && GameSettings.isKeyDown(UNDO);
    }

    private UniversalWandKeyHandler() {}

    public static void init() {
        UniversalWandKeyHandler handler = new UniversalWandKeyHandler();
        ClientRegistry.registerKeyBinding(OPEN_GUI);
        ClientRegistry.registerKeyBinding(FORCE_BEDROCK);
        ClientRegistry.registerKeyBinding(CHAIN);
        ClientRegistry.registerKeyBinding(UNDO);
        ClientRegistry.registerKeyBinding(FLIGHT_SPEED);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
    }

    @SubscribeEvent
    public void onScroll(MouseEvent event) {
        if (event.dwheel == 0 || event.isCanceled()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != null || minecraft.thePlayer == null
            || !minecraft.inGameHasFocus
            || !(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) return;
        ItemStack stack = minecraft.thePlayer.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemUniversalWand)
            || !ItemUniversalWand.isTimeAccelerationEnabled(stack)) return;
        event.setCanceled(true);
        UniversalWandNetwork.requestAccelerationManagement(event.dwheel > 0 ? 1 : 0, 0, 0, 0, 0);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null) {
            UniversalWandUtilityHandler.receiveClientSpeed(null, 0, false);
            lastChainHeld = false;
            lastUndoHeld = false;
            return;
        }
        ItemStack stack = minecraft.thePlayer.getHeldItem();
        boolean undo = isUndoKeyHeld() && ItemUniversalWand.isHeldBy(minecraft.thePlayer)
            && ItemUniversalWand.isBuildingEnabled(stack);
        if (undo != lastUndoHeld || undo && ++undoHeartbeat >= 10) {
            UniversalWandNetwork.requestUndoKey(undo);
            lastUndoHeld = undo;
            undoHeartbeat = 0;
        }
        boolean held = minecraft.currentScreen == null && minecraft.inGameHasFocus
            && isChainKeyHeld()
            && stack != null
            && stack.getItem() instanceof ItemUniversalWand;
        if (held != lastChainHeld || (held && ++chainHeartbeat >= 10)) {
            UniversalWandNetwork.requestChainKey(held);
            lastChainHeld = held;
            chainHeartbeat = 0;
        }
    }

    @SubscribeEvent
    public void onMouseInput(MouseInputEvent event) {
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != null || minecraft.thePlayer == null) return;
        ItemStack stack = minecraft.thePlayer.getHeldItem();
        if (isChainKeyHeld() && stack != null && stack.getItem() instanceof ItemUniversalWand) {
            UniversalWandNetwork.requestChainKey(true);
            if (minecraft.objectMouseOver != null
                && minecraft.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                UniversalWandNetwork.requestChainStart(
                    minecraft.objectMouseOver.blockX,
                    minecraft.objectMouseOver.blockY,
                    minecraft.objectMouseOver.blockZ);
            }
            lastChainHeld = true;
            chainHeartbeat = 0;
        }
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != null || minecraft.thePlayer == null) return;
        if (FLIGHT_SPEED.isPressed() && UniversalWandFeature.FLIGHT.allowed()) UniversalWandNetwork
            .requestFlightSpeed(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        ItemStack stack = minecraft.thePlayer.getHeldItem();
        boolean chainHeld = isChainKeyHeld() && stack != null && stack.getItem() instanceof ItemUniversalWand;
        if (chainHeld != lastChainHeld) {
            UniversalWandNetwork.requestChainKey(chainHeld);
            lastChainHeld = chainHeld;
            chainHeartbeat = 0;
        }
        if (stack == null || !(stack.getItem() instanceof ItemUniversalWand)) return;
        if (OPEN_GUI.isPressed()) UniversalWandNetwork.requestOpenGui(minecraft.thePlayer.inventory.currentItem);
        if (FORCE_BEDROCK.isPressed()) {
            MovingObjectPosition target = minecraft.objectMouseOver;
            if (target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                UniversalWandNetwork.requestForceBedrock(target.blockX, target.blockY, target.blockZ);
            }
        }
    }
}
