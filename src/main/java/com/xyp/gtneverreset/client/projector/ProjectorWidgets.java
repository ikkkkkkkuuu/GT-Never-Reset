package com.xyp.gtneverreset.client.projector;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProjectorWidgets {

    public static final String MENU = "multiblock_projector_menu_";
    public static final int ROW = 18, BUTTON = 20, SMALL = 16, LINE = 10, CONTROL = 84, MENU_WIDTH = 150;

    private ProjectorWidgets() {}

    public static Flow column() {
        return Flow.column()
            .crossAxisAlignment(Alignment.CrossAxis.START);
    }

    public static Widget<?> text(String text) {
        return IKey.str(text)
            .asWidget()
            .alignment(Alignment.CenterLeft)
            .height(LINE);
    }

    public static ParentWidget<?> overlay(Widget<?> first, Widget<?> second) {
        return new ParentWidget<>().size(CONTROL, SMALL)
            .child(first.pos(0, 0))
            .child(second.pos(0, 0));
    }

    public static String trim(String text, int width) {
        return Minecraft.getMinecraft().fontRenderer.trimStringToWidth(text, Math.max(0, width));
    }
}
