package com.xyp.gtneverreset.client.script.radialmenu;

import java.util.List;

import com.xyp.gtnrlib.gui.wheel.AccentCoefficients;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface ScriptContext {

    String playerName();

    int dimension();

    int blockX();

    int blockY();

    int blockZ();

    double yaw();

    double pitch();

    double health();

    int food();

    int air();

    ScriptItem heldItem();

    List<ScriptItem> inventory();

    LookTarget lookingAt();

    long worldTime();

    String worldName();

    String storeGet(String key);

    AccentCoefficients accentCoefficients();
}
