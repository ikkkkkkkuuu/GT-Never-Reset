package com.xyp.gtneverreset.utils.machine.wireless;

import java.util.Map;

import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.utils.machine.wireless.WirelessCopiedLinks.CopiedLink;

public interface IWirelessCopiedLink {

    Map<ForgeDirection, CopiedLink> gtnr$copiedLinks();

    void gtnr$setCopiedLinks(Map<ForgeDirection, CopiedLink> links);
}
