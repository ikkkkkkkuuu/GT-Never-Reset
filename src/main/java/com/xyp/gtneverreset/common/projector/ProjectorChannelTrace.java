package com.xyp.gtneverreset.common.projector;

import java.util.Set;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

// StructureLib reads every channel through this compound, so the names asked for are the machine's channels.
public class ProjectorChannelTrace extends NBTTagCompound {

    public final Set<String> queried;

    public ProjectorChannelTrace(Set<String> queried) {
        this.queried = queried;
    }

    @Override
    public boolean hasKey(String key) {
        queried.add(key);
        return super.hasKey(key);
    }

    @Override
    public boolean hasKey(String key, int type) {
        queried.add(key);
        return super.hasKey(key, type);
    }

    @Override
    public int getInteger(String key) {
        queried.add(key);
        return super.getInteger(key);
    }

    @Override
    public NBTBase copy() {
        ProjectorChannelTrace copy = new ProjectorChannelTrace(queried);
        for (Object key : func_150296_c()) {
            String name = (String) key;
            copy.setTag(name, getTag(name).copy());
        }
        return copy;
    }
}
