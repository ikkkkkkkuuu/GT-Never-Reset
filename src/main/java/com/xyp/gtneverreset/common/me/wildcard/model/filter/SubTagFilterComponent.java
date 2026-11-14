package com.xyp.gtneverreset.common.me.wildcard.model.filter;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.common.me.wildcard.model.WildcardMaterials;

import gregtech.api.enums.Materials;
import gregtech.api.enums.SubTag;
import lombok.Getter;
import lombok.Setter;

public final class SubTagFilterComponent extends AbstractFilterComponent {

    public static final String TYPE = "subtag";

    private static final String KEY_SUBTAG = "SubTag";
    private static final String KEY_EXAMPLE = "Example";

    @Getter
    @Setter
    private SubTag subTag;
    /** 示例材料（拖入的物品解析出来），仅用于 GUI 列出可选 SubTag，不参与匹配。 */
    @Getter
    private Materials example;
    @Getter
    private String exampleName;

    public SubTagFilterComponent(SubTag subTag, Materials example, boolean whitelist) {
        super(whitelist);
        this.subTag = subTag;
        this.example = example;
        this.exampleName = WildcardMaterials.isRealMaterial(example) ? example.mName : null;
    }

    public static SubTagFilterComponent empty() {
        return new SubTagFilterComponent(null, null, true);
    }

    public static SubTagFilterComponent readData(NBTTagCompound data) {
        SubTag subTag = WildcardMaterials.findSubTag(data.getString(KEY_SUBTAG));
        SubTagFilterComponent component = new SubTagFilterComponent(subTag, null, readWhitelist(data));
        component.exampleName = data.getString(KEY_EXAMPLE);
        component.example = WildcardMaterials.findByName(component.exampleName);
        if (!WildcardMaterials.isRealMaterial(component.example)) component.example = null;
        return component;
    }

    /** 设置示例材料，并把当前 SubTag 对齐到该材料实际拥有的第一个 SubTag（若当前的它没有）。 */
    public void setExample(Materials example) {
        setExampleName(WildcardMaterials.isRealMaterial(example) ? example.mName : null);
    }

    public void setExampleName(String exampleName) {
        this.exampleName = exampleName == null || exampleName.isEmpty() ? null : exampleName;
        Materials material = WildcardMaterials.findByName(this.exampleName);
        this.example = WildcardMaterials.isRealMaterial(material) ? material : null;
        if (this.exampleName != null) {
            List<SubTag> avail = WildcardMaterials.subTagsOf(this.exampleName);
            if (!avail.isEmpty() && (subTag == null || !avail.contains(subTag))) {
                subTag = avail.get(0);
            }
        }
    }

    @Override
    protected boolean matches(Materials material) {
        return WildcardMaterials.hasSubTag(material, subTag);
    }

    @Override
    public boolean test(String materialName) {
        return materialName != null && isWhitelist() == WildcardMaterials.hasSubTag(materialName, subTag);
    }

    @Override
    public String describe() {
        return (isWhitelist() ? "+" : "-") + (subTag == null ? "?" : subTag.mName);
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = baseData();
        data.setString(KEY_SUBTAG, subTag == null ? "" : subTag.mName);
        if (exampleName != null) data.setString(KEY_EXAMPLE, exampleName);
        return data;
    }
}
