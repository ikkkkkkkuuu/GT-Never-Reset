package com.xyp.gtneverreset.common.me.wildcard.model;

import gregtech.api.enums.Materials;

public interface IWildcardFilterComponent extends IWildcardComponent {

    /**
     * 该材料是否通过本过滤组件。
     * 实现应写成 {@code return isWhitelist() == matches(material);}
     */
    boolean test(Materials material);

    /** Checks an ore-dictionary material that has no GT Materials entry. */
    default boolean test(String materialName) {
        Materials material = WildcardMaterials.findByName(materialName);
        return WildcardMaterials.isRealMaterial(material) && test(material);
    }

    boolean isWhitelist();

    void setWhitelist(boolean whitelist);

    /** 供 GUI 显示的简短描述。 */
    String describe();
}
