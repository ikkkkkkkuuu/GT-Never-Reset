package com.xyp.gtneverreset.utils.recipes.wildcard.model;

import net.minecraft.nbt.NBTTagCompound;

public interface IWildcardComponent {

    /** 序列化键（类型标识），如 "prefix" / "fluid" / "simple"。 */
    String typeKey();

    /** 把组件自身状态写入 data 复合标签。 */
    NBTTagCompound writeData();
}
