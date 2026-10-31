package com.xyp.gtnrlib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtnrlib.client.fx.world.FxEffects;
import com.xyp.gtnrlib.client.fx.world.FxMoteRenderer;
import com.xyp.gtnrlib.client.hud.HudLayout;
import com.xyp.gtnrlib.client.hud.HudOverlay;
import com.xyp.gtnrlib.client.shader.ShaderEffect;
import com.xyp.gtnrlib.client.text.EffectTextRenderer;
import com.xyp.gtnrlib.client.text.effect.BuiltinTextEffects;
import com.xyp.gtnrlib.client.text.preview.TextEffectPreviewCommand;
import com.xyp.gtnrlib.config.GTNRLibConfig;
import com.xyp.gtnrlib.gui.theme.PaletteThemes;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GTNRLibClient {

    public static void preInit() {
        FxEffects.register();
        GTNRLibConfig.register();
        connectConfig();
        HudOverlay.register();
        MinecraftForge.EVENT_BUS.register(new ShaderEffect.Reloader());
        MinecraftForge.EVENT_BUS.register(new PaletteThemes.Reloader());
        BuiltinTextEffects.register();
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(EffectTextRenderer.INSTANCE);
        ClientCommandHandler.instance.registerCommand(new TextEffectPreviewCommand());
    }

    public static void connectConfig() {
        PaletteThemes.chooseWith(() -> GTNRLibConfig.theme);
        PaletteThemes.onRegister(() -> GTNRLibConfig.listThemes(PaletteThemes.ids()));
        HudLayout.settings(() -> GTNRLibConfig.enableHud, () -> GTNRLibConfig.hudScale);
        FxMoteRenderer.limitWith(() -> GTNRLibConfig.maxWorldMotes);
        EffectTextRenderer.enabledWith(() -> GTNRLibConfig.enableTextEffects);
    }
}
