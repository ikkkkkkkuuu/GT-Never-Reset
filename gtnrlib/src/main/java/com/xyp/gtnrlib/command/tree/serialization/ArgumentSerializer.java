package com.xyp.gtnrlib.command.tree.serialization;

import net.minecraft.network.PacketBuffer;

import com.xyp.gtnrlib.command.brigadier.arguments.ArgumentType;

public interface ArgumentSerializer<T extends ArgumentType<?>> {

    void serializeToNetwork(T var1, PacketBuffer var2);

    T deserializeFromNetwork(PacketBuffer var1);
}
