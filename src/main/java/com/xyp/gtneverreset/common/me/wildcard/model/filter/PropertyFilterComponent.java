package com.xyp.gtneverreset.common.me.wildcard.model.filter;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.common.me.wildcard.model.WildcardMaterials;

import gregtech.api.enums.Materials;
import lombok.Getter;
import lombok.Setter;

public final class PropertyFilterComponent extends AbstractFilterComponent {

    public static final String TYPE = "property";

    private static final String KEY_PROPERTY = "Property";
    private static final String KEY_EXAMPLE = "Example";

    @Getter
    @Setter
    private WildcardMaterials.Property property;
    /** 示例材料（拖入的物品解析出来），仅用于 GUI 列出可选属性，不参与匹配。 */
    @Getter
    private Materials example;
    @Getter
    private String exampleName;

    public PropertyFilterComponent(WildcardMaterials.Property property, Materials example, boolean whitelist) {
        super(whitelist);
        this.property = property;
        this.example = example;
        this.exampleName = WildcardMaterials.isRealMaterial(example) ? example.mName : null;
    }

    public static PropertyFilterComponent empty() {
        return new PropertyFilterComponent(WildcardMaterials.Property.Metal, null, true);
    }

    public static PropertyFilterComponent readData(NBTTagCompound data) {
        WildcardMaterials.Property property = WildcardMaterials.findProperty(data.getString(KEY_PROPERTY));
        PropertyFilterComponent component = new PropertyFilterComponent(property, null, readWhitelist(data));
        component.exampleName = data.getString(KEY_EXAMPLE);
        component.example = WildcardMaterials.findByName(component.exampleName);
        if (!WildcardMaterials.isRealMaterial(component.example)) component.example = null;
        return component;
    }

    /** 设置示例材料，并把当前属性对齐到该材料实际拥有的第一个属性（若当前属性它没有）。 */
    public void setExample(Materials example) {
        setExampleName(WildcardMaterials.isRealMaterial(example) ? example.mName : null);
    }

    public void setExampleName(String exampleName) {
        this.exampleName = exampleName == null || exampleName.isEmpty() ? null : exampleName;
        Materials material = WildcardMaterials.findByName(this.exampleName);
        this.example = WildcardMaterials.isRealMaterial(material) ? material : null;
        if (this.exampleName != null) {
            List<WildcardMaterials.Property> avail = WildcardMaterials.propertiesOf(this.exampleName);
            if (!avail.isEmpty() && (property == null || !avail.contains(property))) {
                property = avail.get(0);
            }
        }
    }

    @Override
    protected boolean matches(Materials material) {
        return property != null && property.test(material);
    }

    @Override
    public boolean test(String materialName) {
        return materialName != null && isWhitelist() == (property != null && property.test(materialName));
    }

    @Override
    public String describe() {
        return (isWhitelist() ? "+" : "-") + (property == null ? "?" : property.serializedName);
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = baseData();
        data.setString(KEY_PROPERTY, property == null ? "" : property.serializedName);
        if (exampleName != null) data.setString(KEY_EXAMPLE, exampleName);
        return data;
    }
}
