package com.xyp.gtneverreset.utils.machine.wireless;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessChannelManager.Address;

@Desugar
public record WirelessCardBinding(WirelessFrequency frequency, Address location, UUID creator, UUID owner,
    String ownerName) {

    private static final String KEY = "GTNLWirelessCard";
    private static final int VERSION = 2;
    private static final String AUTO_KEY = "GTNLWirelessAutoConnect";
    private static final String GROUP_KEY = "GTNLWirelessCollapseTypes";
    private static final String ID_KEY = "GTNLWirelessCardIdentity";
    private static final String SOURCES_KEY = "GTNGWirelessSources";
    public static final int MAX_SOURCES = 64;

    static UUID identity(NBTTagCompound itemTag) {
        if (itemTag == null || !itemTag.hasKey(ID_KEY, 8)) return null;
        try {
            return UUID.fromString(itemTag.getString(ID_KEY));
        } catch (IllegalArgumentException invalidId) {
            return null;
        }
    }

    static boolean identify(ItemStack card) {
        if (identity(card.getTagCompound()) != null) return false;
        ItemStackNBT.get(card)
            .setString(
                ID_KEY,
                UUID.randomUUID()
                    .toString());
        return true;
    }

    public static boolean automatic(NBTTagCompound itemTag) {
        return itemTag != null && itemTag.getBoolean(AUTO_KEY);
    }

    public static boolean toggleAutomatic(ItemStack card) {
        boolean enabled = ItemStackNBT.invertBoolean(card, AUTO_KEY);
        rememberCurrent(ItemStackNBT.get(card));
        return enabled;
    }

    public static boolean collapseTypes(NBTTagCompound itemTag) {
        return itemTag == null || !itemTag.hasKey(GROUP_KEY, 1) || itemTag.getBoolean(GROUP_KEY);
    }

    public static boolean toggleGrouping(ItemStack card) {
        boolean collapse = !collapseTypes(card.getTagCompound());
        ItemStackNBT.setBoolean(card, GROUP_KEY, collapse);
        return collapse;
    }

    public static WirelessCardBinding read(NBTTagCompound itemTag) {
        return itemTag == null || !itemTag.hasKey(KEY, 10) ? null : decode(itemTag.getCompoundTag(KEY));
    }

    private static WirelessCardBinding decode(NBTTagCompound data) {
        int version = data.getInteger("version");
        Address location = WirelessLinkData.readPosition(data);
        UUID owner = WirelessFrequencies.uuid(data, "owner");
        if (!data.hasKey("version", 3) || location == null || owner == null) return null;
        WirelessFrequency frequency;
        if (version == 1) frequency = WirelessFrequency.legacy(location);
        else if (version == VERSION && WirelessFrequencies.uuid(data, "frequency") != null)
            frequency = WirelessFrequency.of(WirelessFrequencies.uuid(data, "frequency"));
        else return null;
        return new WirelessCardBinding(
            frequency,
            location,
            WirelessFrequencies.uuid(data, "creator"),
            owner,
            data.getString("ownerName"));
    }

    public void write(NBTTagCompound itemTag) {
        rememberCurrent(itemTag);
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        int index = indexOf(entries, this);
        if (index < 0 && entries.tagCount() >= MAX_SOURCES) throw new IllegalStateException("Source limit reached");
        boolean automatic = index >= 0 && entries.getCompoundTagAt(index)
            .getBoolean("automatic");
        NBTTagCompound data = encode();
        if (index >= 0) data.setString(
            "name",
            entries.getCompoundTagAt(index)
                .getString("name"));
        data.setBoolean("automatic", automatic);
        if (index < 0) entries.appendTag(data);
        else entries.func_150304_a(index, data);
        itemTag.setTag(SOURCES_KEY, entries);
        itemTag.setTag(KEY, encode());
        itemTag.setBoolean(AUTO_KEY, automatic);
    }

    private NBTTagCompound encode() {
        NBTTagCompound data = WirelessLinkData.writePosition(new NBTTagCompound(), location);
        data.setInteger("version", frequency.isLegacy() ? 1 : VERSION);
        if (!frequency.isLegacy()) data.setString(
            "frequency",
            frequency.id()
                .toString());
        if (creator != null) data.setString("creator", creator.toString());
        data.setString("owner", owner.toString());
        data.setString("ownerName", ownerName);
        return data;
    }

    public static List<WirelessCardBinding> sources(NBTTagCompound itemTag) {
        List<WirelessCardBinding> result = new ArrayList<>();
        WirelessCardBinding current = read(itemTag);
        if (current == null) return result;
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        for (int i = 0; i < Math.min(entries.tagCount(), MAX_SOURCES); i++) {
            NBTTagCompound wrapper = new NBTTagCompound();
            wrapper.setTag(KEY, entries.getCompoundTagAt(i));
            WirelessCardBinding binding = read(wrapper);
            if (binding != null && binding.owner()
                .equals(current.owner()) && !result.contains(binding)) result.add(binding);
        }
        if (result.isEmpty()) result.add(current);
        return result;
    }

    public static boolean select(NBTTagCompound itemTag, int index) {
        List<WirelessCardBinding> saved = sources(itemTag);
        if (index < 0 || index >= saved.size()) return false;
        saved.get(index)
            .write(itemTag);
        return true;
    }

    public static String sourceName(NBTTagCompound itemTag, WirelessCardBinding binding) {
        if (itemTag == null || binding == null) return "";
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        int index = indexOf(entries, binding);
        return index < 0 ? ""
            : entries.getCompoundTagAt(index)
                .getString("name");
    }

    public static boolean renameSource(NBTTagCompound itemTag, int index, String name) {
        if (name == null || name.length() > 32) return false;
        for (int i = 0; i < name.length(); i++)
            if (Character.isISOControl(name.charAt(i)) || name.charAt(i) == '\u00a7') return false;
        List<WirelessCardBinding> saved = sources(itemTag);
        if (index < 0 || index >= saved.size()) return false;
        rememberCurrent(itemTag);
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        int storedIndex = indexOf(entries, saved.get(index));
        if (storedIndex < 0) return false;
        entries.getCompoundTagAt(storedIndex)
            .setString("name", name.trim());
        return true;
    }

    private static int indexOf(NBTTagList entries, WirelessCardBinding binding) {
        for (int i = 0; i < entries.tagCount(); i++) {
            WirelessCardBinding saved = decode(entries.getCompoundTagAt(i));
            if (saved != null && saved.frequency()
                .equals(binding.frequency())
                && saved.owner()
                    .equals(binding.owner()))
                return i;
        }
        return -1;
    }

    private static void rememberCurrent(NBTTagCompound itemTag) {
        WirelessCardBinding current = read(itemTag);
        if (current == null) return;
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        int index = indexOf(entries, current);
        NBTTagCompound data = current.encode();
        if (index >= 0) data.setString(
            "name",
            entries.getCompoundTagAt(index)
                .getString("name"));
        data.setBoolean("automatic", automatic(itemTag));
        if (index >= 0) entries.func_150304_a(index, data);
        else if (entries.tagCount() < MAX_SOURCES) entries.appendTag(data);
        itemTag.setTag(SOURCES_KEY, entries);
    }

    public static void clear(NBTTagCompound itemTag) {
        if (itemTag == null) return;
        rememberCurrent(itemTag);
        WirelessCardBinding current = read(itemTag);
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        int index = current == null ? -1 : indexOf(entries, current);
        if (index >= 0) entries.removeTag(index);
        itemTag.removeTag(KEY);
        itemTag.setBoolean(AUTO_KEY, false);
        if (entries.tagCount() > 0) {
            NBTTagCompound next = entries.getCompoundTagAt(0);
            itemTag.setTag(KEY, next.copy());
            itemTag.setBoolean(AUTO_KEY, next.getBoolean("automatic"));
        }
    }

    public static boolean removeSource(NBTTagCompound itemTag, int index) {
        List<WirelessCardBinding> saved = sources(itemTag);
        if (index < 0 || index >= saved.size()) return false;
        WirelessCardBinding removed = saved.get(index);
        if (removed.equals(read(itemTag))) clear(itemTag);
        else {
            rememberCurrent(itemTag);
            NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
            int storedIndex = indexOf(entries, removed);
            if (storedIndex < 0) return false;
            entries.removeTag(storedIndex);
        }
        return true;
    }

    public static boolean upgrade(ItemStack card, EntityPlayer holder) {
        if (holder.worldObj.isRemote || card.getTagCompound() == null) return false;
        boolean changed = upgrade(card.getTagCompound());
        if (changed) WirelessCardInventory.sync(holder, card);
        return changed;
    }

    static boolean upgrade(NBTTagCompound tag) {
        boolean changed = upgradeEntry(tag.getCompoundTag(KEY));
        NBTTagList entries = tag.getTagList(SOURCES_KEY, 10);
        for (int i = 0; i < Math.min(entries.tagCount(), MAX_SOURCES); i++)
            changed |= upgradeEntry(entries.getCompoundTagAt(i));
        return changed;
    }

    private static boolean upgradeEntry(NBTTagCompound data) {
        WirelessCardBinding binding = decode(data);
        if (binding == null) return false;
        WirelessFrequency frequency = WirelessFrequencies.upgrade(binding.frequency());
        if (frequency.isLegacy()) return false;
        Address controller = WirelessFrequencies.controller(frequency);
        WirelessCardBinding updated = new WirelessCardBinding(
            frequency,
            controller == null ? binding.location() : controller,
            WirelessFrequencies.creator(frequency),
            binding.owner(),
            binding.ownerName());
        if (binding.equals(updated)) return false;
        NBTTagCompound encoded = updated.encode();
        for (Object key : encoded.func_150296_c()) data.setTag((String) key, encoded.getTag((String) key));
        return true;
    }

    public boolean belongsTo(UUID player) {
        return owner.equals(player);
    }

    boolean sameIdentity(WirelessCardBinding other) {
        return other != null && owner.equals(other.owner())
            && WirelessFrequencies.upgrade(frequency)
                .equals(WirelessFrequencies.upgrade(other.frequency()));
    }
}
