package com.xyp.gtnrlib.command.tree.serialization;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Maps;
import com.xyp.gtnrlib.command.brigadier.arguments.ArgumentType;
import com.xyp.gtnrlib.command.brigadier.arguments.BoolArgumentType;
import com.xyp.gtnrlib.command.brigadier.arguments.DoubleArgumentType;
import com.xyp.gtnrlib.command.brigadier.arguments.FloatArgumentType;
import com.xyp.gtnrlib.command.brigadier.arguments.IntegerArgumentType;
import com.xyp.gtnrlib.command.brigadier.arguments.LongArgumentType;
import com.xyp.gtnrlib.command.brigadier.arguments.StringArgumentType;

public class ArgumentTypes {

    private static final Map<Class<?>, ArgumentTypes.Entry<?>> BY_CLASS = Maps.newHashMap();
    private static final Map<ResourceLocation, ArgumentTypes.Entry<?>> BY_NAME = Maps.newHashMap();

    public static <T extends ArgumentType<?>> void register(String pName, Class<T> pClazz,
        ArgumentSerializer<T> pSerializer) {
        ResourceLocation resourcelocation = new ResourceLocation(pName);
        if (BY_CLASS.containsKey(pClazz)) {
            throw new IllegalArgumentException("Class " + pClazz.getName() + " already has a serializer!");
        } else if (BY_NAME.containsKey(resourcelocation)) {
            throw new IllegalArgumentException("'" + resourcelocation + "' is already a registered serializer!");
        } else {
            ArgumentTypes.Entry<T> entry = new ArgumentTypes.Entry<T>(pClazz, pSerializer, resourcelocation);
            BY_CLASS.put(pClazz, entry);
            BY_NAME.put(resourcelocation, entry);
        }
    }

    public static void init() {
        register(
            "brigadier:bool",
            BoolArgumentType.class,
            new EmptyArgumentSerializer<BoolArgumentType>(BoolArgumentType::bool));
        register("brigadier:float", FloatArgumentType.class, new FloatArgumentSerializer());
        register("brigadier:double", DoubleArgumentType.class, new DoubleArgumentSerializer());
        register("brigadier:integer", IntegerArgumentType.class, new IntegerArgumentSerializer());
        register("brigadier:long", LongArgumentType.class, new LongArgumentSerializer());
        register("brigadier:string", StringArgumentType.class, new StringArgumentSerializer());
    }

    @Nullable
    private static ArgumentTypes.Entry<?> get(ResourceLocation pType) {
        return BY_NAME.get(pType);
    }

    @Nullable
    private static ArgumentTypes.Entry<?> get(ArgumentType<?> pType) {
        return BY_CLASS.get(pType.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T extends ArgumentType<?>> void serialize(PacketBuffer pBuffer, T pType) throws IOException {
        ArgumentTypes.Entry<T> entry = (ArgumentTypes.Entry<T>) get(pType);
        if (entry == null) {
            writeResourceLocation(pBuffer, new ResourceLocation(""));
        } else {
            writeResourceLocation(pBuffer, entry.name);
            entry.serializer.serializeToNetwork(pType, pBuffer);
        }
    }

    @Nullable
    public static ArgumentType<?> deserialize(PacketBuffer pBuffer) throws IOException {
        ResourceLocation resourcelocation = readResourceLocation(pBuffer);
        ArgumentTypes.Entry<?> entry = get(resourcelocation);
        return entry == null ? null : entry.serializer.deserializeFromNetwork(pBuffer);
    }

    public static void writeResourceLocation(PacketBuffer buffer, ResourceLocation loc) throws IOException {
        buffer.writeStringToBuffer(loc.toString());
    }

    public static ResourceLocation readResourceLocation(PacketBuffer buffer) throws IOException {
        return new ResourceLocation(buffer.readStringFromBuffer(32767));
    }

    static class Entry<T extends ArgumentType<?>> {

        public final Class<T> clazz;
        public final ArgumentSerializer<T> serializer;
        public final ResourceLocation name;

        Entry(Class<T> pClazz, ArgumentSerializer<T> pSerializer, ResourceLocation pName) {
            this.clazz = pClazz;
            this.serializer = pSerializer;
            this.name = pName;
        }
    }

    private static byte createNumberFlags(boolean min, boolean max) {
        return (byte) ((min ? 1 : 0) | (max ? 2 : 0));
    }

    private static boolean numberHasMin(byte flags) {
        return (flags & 1) != 0;
    }

    private static boolean numberHasMax(byte flags) {
        return (flags & 2) != 0;
    }

    private static class DoubleArgumentSerializer implements ArgumentSerializer<DoubleArgumentType> {

        public void serializeToNetwork(DoubleArgumentType pArgument, PacketBuffer pBuffer) {
            boolean flag = pArgument.getMinimum() != -Double.MAX_VALUE;
            boolean flag1 = pArgument.getMaximum() != Double.MAX_VALUE;
            pBuffer.writeByte(createNumberFlags(flag, flag1));
            if (flag) {
                pBuffer.writeDouble(pArgument.getMinimum());
            }

            if (flag1) {
                pBuffer.writeDouble(pArgument.getMaximum());
            }
        }

        public DoubleArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
            byte b0 = pBuffer.readByte();
            double d0 = numberHasMin(b0) ? pBuffer.readDouble() : -Double.MAX_VALUE;
            double d1 = numberHasMax(b0) ? pBuffer.readDouble() : Double.MAX_VALUE;
            return DoubleArgumentType.doubleArg(d0, d1);
        }
    }

    private static class EmptyArgumentSerializer<T extends ArgumentType<?>> implements ArgumentSerializer<T> {

        private final Supplier<T> constructor;

        public EmptyArgumentSerializer(Supplier<T> pConstructor) {
            this.constructor = pConstructor;
        }

        @Override
        public void serializeToNetwork(T pArgument, PacketBuffer pBuffer) {}

        @Override
        public T deserializeFromNetwork(PacketBuffer pBuffer) {
            return this.constructor.get();
        }
    }

    private static class FloatArgumentSerializer implements ArgumentSerializer<FloatArgumentType> {

        public void serializeToNetwork(FloatArgumentType pArgument, PacketBuffer pBuffer) {
            boolean flag = pArgument.getMinimum() != -Float.MAX_VALUE;
            boolean flag1 = pArgument.getMaximum() != Float.MAX_VALUE;
            pBuffer.writeByte(createNumberFlags(flag, flag1));
            if (flag) {
                pBuffer.writeFloat(pArgument.getMinimum());
            }

            if (flag1) {
                pBuffer.writeFloat(pArgument.getMaximum());
            }
        }

        public FloatArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
            byte b0 = pBuffer.readByte();
            float f = numberHasMin(b0) ? pBuffer.readFloat() : -Float.MAX_VALUE;
            float f1 = numberHasMax(b0) ? pBuffer.readFloat() : Float.MAX_VALUE;
            return FloatArgumentType.floatArg(f, f1);
        }
    }

    private static class IntegerArgumentSerializer implements ArgumentSerializer<IntegerArgumentType> {

        public void serializeToNetwork(IntegerArgumentType pArgument, PacketBuffer pBuffer) {
            boolean flag = pArgument.getMinimum() != Integer.MIN_VALUE;
            boolean flag1 = pArgument.getMaximum() != Integer.MAX_VALUE;
            pBuffer.writeByte(createNumberFlags(flag, flag1));
            if (flag) {
                pBuffer.writeInt(pArgument.getMinimum());
            }

            if (flag1) {
                pBuffer.writeInt(pArgument.getMaximum());
            }
        }

        public IntegerArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
            byte b0 = pBuffer.readByte();
            int i = numberHasMin(b0) ? pBuffer.readInt() : Integer.MIN_VALUE;
            int j = numberHasMax(b0) ? pBuffer.readInt() : Integer.MAX_VALUE;
            return IntegerArgumentType.integer(i, j);
        }
    }

    private static class LongArgumentSerializer implements ArgumentSerializer<LongArgumentType> {

        public void serializeToNetwork(LongArgumentType pArgument, PacketBuffer pBuffer) {
            boolean flag = pArgument.getMinimum() != Long.MIN_VALUE;
            boolean flag1 = pArgument.getMaximum() != Long.MAX_VALUE;
            pBuffer.writeByte(createNumberFlags(flag, flag1));
            if (flag) {
                pBuffer.writeLong(pArgument.getMinimum());
            }

            if (flag1) {
                pBuffer.writeLong(pArgument.getMaximum());
            }
        }

        public LongArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
            byte b0 = pBuffer.readByte();
            long i = numberHasMin(b0) ? pBuffer.readLong() : Long.MIN_VALUE;
            long j = numberHasMax(b0) ? pBuffer.readLong() : Long.MAX_VALUE;
            return LongArgumentType.longArg(i, j);
        }
    }

    private static class StringArgumentSerializer implements ArgumentSerializer<StringArgumentType> {

        public void serializeToNetwork(StringArgumentType pArgument, PacketBuffer pBuffer) {
            pBuffer.writeVarIntToBuffer(
                pArgument.getType()
                    .ordinal());
        }

        public StringArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
            int ordinal = pBuffer.readVarIntFromBuffer();
            StringArgumentType.StringType[] values = StringArgumentType.StringType.values();
            StringArgumentType.StringType stringtype = ordinal >= 0 && ordinal < values.length ? values[ordinal]
                : StringArgumentType.StringType.GREEDY_PHRASE;
            switch (stringtype) {
                case SINGLE_WORD:
                    return StringArgumentType.word();
                case QUOTABLE_PHRASE:
                    return StringArgumentType.string();
                case GREEDY_PHRASE:
                default:
                    return StringArgumentType.greedyString();
            }
        }
    }

}
