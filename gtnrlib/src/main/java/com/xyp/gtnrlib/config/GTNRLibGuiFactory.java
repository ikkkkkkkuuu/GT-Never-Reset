package com.xyp.gtnrlib.config;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.client.hud.HudEditor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GTNRLibGuiFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return Screen.class;
    }

    public static class Screen extends SimpleGuiConfig {

        private GuiButton editHud;

        public Screen(GuiScreen parent) throws ConfigException {
            super(parent, GTNRLib.MODID, GTNRLib.NAME, true, GTNRLibConfig.class);
        }

        @Override
        public void initGui() {
            super.initGui();
            editHud = new GuiButton(731, width - 106, 4, 100, 20, StatCollector.translateToLocal(HudEditor.OPEN_LANG));
            buttonList.add(editHud);
        }

        @Override
        protected void actionPerformed(GuiButton button) {
            if (button == editHud) HudEditor.open();
            else super.actionPerformed(button);
        }
    }
}
