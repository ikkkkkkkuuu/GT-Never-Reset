package com.xyp.gtneverreset.common.gui;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.nbt.NBTTagCompound;

import com.glodblock.github.client.gui.GuiFCImgButton;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.TerminalMagnetPacket;
import com.xyp.gtneverreset.common.terminal.WirelessTerminalMagnet;

import appeng.helpers.ICustomButtonDataObject;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public final class WirelessTerminalMagnetGui implements ICustomButtonDataObject {

    private int mode = 1;
    @SideOnly(Side.CLIENT)
    private GuiFCImgButton[] modes;
    @SideOnly(Side.CLIENT)
    private GuiFCImgButton filter;

    public void cycleMode() {
        mode = (mode + 1) % 3;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void initCustomButtons(int left, int top, int width, int height, int xOffset, int yOffset,
        List<GuiButton> buttons) {
        modes = new GuiFCImgButton[3];
        String[] values = { "OFF", "INV", "ME" };
        for (int i = 0; i < modes.length; i++) {
            modes[i] = new GuiFCImgButton(left + width - 17, top + height - 89, "MAGNET_CARD", values[i]);
            buttons.add(modes[i]);
        }
        filter = new GuiFCImgButton(left + width - 17, top + height - 69, "MAGNET_CARD", "FILTER");
        buttons.add(filter);
        updateVisibility();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean actionPerformedCustomButtons(GuiButton button) {
        if (button == filter) {
            NetWorkHandler.sendToServer(new TerminalMagnetPacket(true));
            return true;
        }
        if (modes != null) for (GuiButton candidate : modes) {
            if (button == candidate) {
                NetWorkHandler.sendToServer(new TerminalMagnetPacket(false));
                return true;
            }
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    public boolean ownsButton(GuiButton button) {
        if (button == filter) return true;
        if (modes != null) for (GuiButton candidate : modes) if (button == candidate) return true;
        return false;
    }

    @SideOnly(Side.CLIENT)
    private void updateVisibility() {
        if (modes != null) for (int i = 0; i < modes.length; i++) modes[i].setVisibility(i == mode);
    }

    @Override
    public void readData(NBTTagCompound tag) {
        mode = tag != null && tag.hasKey(WirelessTerminalMagnet.MODE_KEY)
            ? Math.max(0, Math.min(2, tag.getInteger(WirelessTerminalMagnet.MODE_KEY)))
            : 1;
    }

    @Override
    public void writeData(NBTTagCompound tag) {
        tag.setInteger(WirelessTerminalMagnet.MODE_KEY, mode);
    }

    @Override
    public void writeByte(ByteBuf buf) {
        buf.writeInt(mode);
    }

    @Override
    public void readByte(ByteBuf buf) {
        mode = Math.max(0, Math.min(2, buf.readInt()));
        updateVisibility();
    }
}
