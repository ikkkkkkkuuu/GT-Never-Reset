package com.xyp.gtneverreset.common.me.wildcard.model;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.Materials;

public interface IWildcardIOComponent extends IWildcardComponent {

    /**
     * 用给定材料生成具体 stack。
     *
     * @param material 当前正在展开的材料（simple 组件可忽略它，返回固定 stack）
     * @return 具体物品/流体 stack，或 null（无法生成）
     */
    ItemStack apply(Materials material);

    /** Resolves an ore-dictionary material that has no GT Materials entry. */
    default ItemStack apply(String materialName) {
        Materials material = WildcardMaterials.findByName(materialName);
        return WildcardMaterials.isRealMaterial(material) ? apply(material) : null;
    }

    /** 用于 GUI 显示的代表性 stack（可为 null）。 */
    ItemStack getDisplayStack();

    /** 是否为空（未配置有效内容）。 */
    boolean isEmpty();
}
