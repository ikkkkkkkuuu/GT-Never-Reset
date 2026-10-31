package com.xyp.gtneverreset.common.me;

import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;
import com.xyp.gtneverreset.GTNeverReset;

import cpw.mods.fml.common.FMLCommonHandler;

/** Carries both interface naming policies so each viewing client can choose independently. */
public final class ChatComponentInterfaceNameSuffix extends AbstractChatComponentCustom {

    private IChatComponent standard;
    private IChatComponent preferred;

    public ChatComponentInterfaceNameSuffix() {}

    public ChatComponentInterfaceNameSuffix(IChatComponent standard, IChatComponent preferred) {
        this.standard = standard;
        this.preferred = preferred;
    }

    @Override
    public String getID() {
        return GTNeverReset.MODID + ":InterfaceNameSuffix";
    }

    @Override
    public JsonElement serialize() {
        JsonObject json = new JsonObject();
        json.add("standard", encode(standard));
        json.add("preferred", encode(preferred));
        return json;
    }

    @Override
    public void deserialize(JsonElement jsonElement) {
        JsonObject json = jsonElement.getAsJsonObject();
        standard = decode(json.get("standard"));
        preferred = decode(json.get("preferred"));
    }

    @Override
    protected AbstractChatComponentCustom copySelf() {
        return new ChatComponentInterfaceNameSuffix(
            standard == null ? null : standard.createCopy(),
            preferred == null ? null : preferred.createCopy());
    }

    @Override
    public String getUnformattedTextForChat() {
        boolean preferOwn = FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient() && GTNeverReset.proxy.preferOwnInterfaceNames();
        return getText(preferOwn);
    }

    /** Resolves the selected policy without modifying either component or using client settings on the server. */
    public String getText(boolean preferOwn) {
        IChatComponent selected = preferOwn ? preferred : standard;
        return selected == null ? "" : selected.getUnformattedText();
    }

    private static JsonElement encode(IChatComponent component) {
        return component == null ? JsonNull.INSTANCE
            : new JsonPrimitive(IChatComponent.Serializer.func_150696_a(component));
    }

    private static IChatComponent decode(JsonElement value) {
        return value == null || value.isJsonNull() ? null
            : IChatComponent.Serializer.func_150699_a(value.getAsString());
    }
}
