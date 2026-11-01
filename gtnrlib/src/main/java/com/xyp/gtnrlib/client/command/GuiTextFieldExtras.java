package com.xyp.gtnrlib.client.command;

import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Extra drawing controls used only by the chat command input. */
@SideOnly(Side.CLIENT)
public interface GuiTextFieldExtras {

    void gtnrlib$chatInput(boolean enabled);

    void gtnrlib$suggestion(@Nullable String var1);

    void gtnrlib$textFormatter(BiFunction<String, Integer, String> var1);

    int gtnrlib$screenX(int var1);
}
