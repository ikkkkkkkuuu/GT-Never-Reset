package com.xyp.gtneverreset.common.packet;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.client.wand.UniversalWandAccelerationRenderer;
import com.xyp.gtneverreset.client.wand.UniversalWandTravelFallback;
import com.xyp.gtneverreset.common.wand.UniversalWandAccelerationHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandActions;
import com.xyp.gtneverreset.common.wand.UniversalWandFeaturePolicy;
import com.xyp.gtneverreset.common.wand.UniversalWandMiningHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandProtectionHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandRightClick;
import com.xyp.gtneverreset.common.wand.UniversalWandUtilityHandler;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/** Dedicated client-to-server action for opening the wand settings screen. */
public final class UniversalWandNetwork {

    private UniversalWandNetwork() {}

    @SideOnly(Side.CLIENT)
    private static void onClient(MessageContext context, Runnable action) {
        var mc = Minecraft.getMinecraft();
        var connection = context.netHandler;
        mc.func_152344_a(() -> { if (mc.getNetHandler() == connection) action.run(); });
    }

    public static void syncFeatures(EntityPlayerMP player, long mask) {
        NetWorkHandler.sendTo(new FeatureMessage(mask), player);
    }

    public static final class FeatureMessage implements IMessage {

        private long mask;

        public FeatureMessage() {}

        FeatureMessage(long mask) {
            this.mask = mask;
        }

        public void fromBytes(ByteBuf buffer) {
            mask = buffer.readLong();
        }

        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(mask);
        }
    }

    public static final class FeatureHandler implements IMessageHandler<FeatureMessage, IMessage> {

        public IMessage onMessage(FeatureMessage message, MessageContext context) {
            onClient(context, () -> { UniversalWandFeaturePolicy.receive(message.mask); });
            return null;
        }
    }

    public static void requestTravelFallback(int id) {
        NetWorkHandler.sendToServer(new TravelFallbackRequest(id));
    }

    public static void allowTravelFallback(EntityPlayerMP player, int id) {
        NetWorkHandler.sendTo(new TravelFallbackGrant(id), player);
    }

    public static void finishTravelFallback(int id) {
        NetWorkHandler.sendToServer(new TravelFallbackFinish(id));
    }

    public static class TravelFallbackRequest implements IMessage {

        int id;

        public TravelFallbackRequest() {}

        TravelFallbackRequest(int id) {
            this.id = id;
        }

        public void fromBytes(ByteBuf buffer) {
            id = buffer.readInt();
        }

        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(id);
        }
    }

    public static final class TravelFallbackGrant extends TravelFallbackRequest {

        public TravelFallbackGrant() {}

        TravelFallbackGrant(int id) {
            super(id);
        }
    }

    static void handleTravelFallbackRequest(TravelFallbackRequest message, EntityPlayerMP player) {
        UniversalWandRightClick.request(player, message.id);
    }

    public static final class TravelFallbackFinish extends TravelFallbackRequest {

        public TravelFallbackFinish() {}

        TravelFallbackFinish(int id) {
            super(id);
        }
    }

    static void handleTravelFallbackFinish(TravelFallbackFinish message, EntityPlayerMP player) {
        UniversalWandRightClick.finish(player, message.id);
    }

    public static final class TravelFallbackGrantHandler implements IMessageHandler<TravelFallbackGrant, IMessage> {

        public IMessage onMessage(TravelFallbackGrant message, MessageContext context) {
            onClient(context, () -> { UniversalWandTravelFallback.allow(message.id); });
            return null;
        }
    }

    public static void syncAccelerationNotice(EntityPlayerMP player, int rate) {
        NetWorkHandler.sendTo(new AccelerationNoticeMessage(rate), player);
    }

    public static final class AccelerationNoticeMessage implements IMessage {

        private int rate;

        public AccelerationNoticeMessage() {}

        AccelerationNoticeMessage(int rate) {
            this.rate = rate;
        }

        public void fromBytes(ByteBuf buffer) {
            rate = buffer.readInt();
        }

        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(rate);
        }
    }

    public static final class AccelerationNoticeHandler
        implements IMessageHandler<AccelerationNoticeMessage, IMessage> {

        public IMessage onMessage(AccelerationNoticeMessage message, MessageContext context) {
            onClient(context, () -> { UniversalWandUtilityHandler.showClientAccelerationNotice(message.rate); });
            return null;
        }
    }

    public static void requestAccelerationManagement(int action, int dimension, int x, int y, int z) {
        NetWorkHandler.sendToServer(new AccelerationManagementMessage(action, dimension, x, y, z));
    }

    public static final class AccelerationManagementMessage implements IMessage {

        private int action, dimension, x, y, z;

        public AccelerationManagementMessage() {}

        AccelerationManagementMessage(int action, int dimension, int x, int y, int z) {
            this.action = action;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void fromBytes(ByteBuf buffer) {
            action = buffer.readInt();
            dimension = buffer.readInt();
            x = buffer.readInt();
            y = buffer.readInt();
            z = buffer.readInt();
        }

        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(action);
            buffer.writeInt(dimension);
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
        }
    }

    static void handleAccelerationManagement(AccelerationManagementMessage message, EntityPlayerMP player) {
        if (message.action >= 0 && message.action <= 4) UniversalWandAccelerationHandler
            .requestManagement(player, message.action, message.dimension, message.x, message.y, message.z);
    }

    public static void requestOpenGui(int slot) {
        NetWorkHandler.sendToServer(new OpenGuiMessage(slot));
    }

    public static void syncAcceleration(EntityPlayerMP player, int dimension, int x, int y, int z, int level,
        int remaining, int mode, boolean throttled) {
        AccelerationSyncMessage message = new AccelerationSyncMessage(
            dimension,
            x,
            y,
            z,
            level,
            remaining,
            mode,
            throttled);
        if (player != null) NetWorkHandler.sendTo(message, player);
        NetWorkHandler.sendToAllAround(message, new NetworkRegistry.TargetPoint(dimension, x, y, z, 64));
    }

    public static final class AccelerationSyncMessage implements IMessage {

        public int dimension, x, y, z, level, remaining, mode;
        public boolean throttled;

        public AccelerationSyncMessage() {}

        AccelerationSyncMessage(int dimension, int x, int y, int z, int level, int remaining, int mode,
            boolean throttled) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
            this.remaining = remaining;
            this.mode = mode;
            this.throttled = throttled;
        }

        public void fromBytes(ByteBuf buffer) {
            dimension = buffer.readInt();
            x = buffer.readInt();
            y = buffer.readInt();
            z = buffer.readInt();
            level = buffer.readInt();
            remaining = buffer.readInt();
            mode = buffer.readInt();
            throttled = buffer.readBoolean();
        }

        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(dimension);
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
            buffer.writeInt(level);
            buffer.writeInt(remaining);
            buffer.writeInt(mode);
            buffer.writeBoolean(throttled);
        }
    }

    public static final class AccelerationSyncHandler implements IMessageHandler<AccelerationSyncMessage, IMessage> {

        public IMessage onMessage(AccelerationSyncMessage message, MessageContext context) {
            onClient(context, () -> { UniversalWandAccelerationRenderer.receive(message); });
            return null;
        }
    }

    public static void syncProtection(EntityPlayerMP player, boolean enabled, boolean hidden) {
        ProtectionSyncMessage message = new ProtectionSyncMessage(player.getUniqueID(), enabled, hidden);
        NetWorkHandler.sendTo(message, player);
        NetWorkHandler.sendToAllAround(
            message,
            new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 512));
    }

    public static final class ProtectionSyncMessage implements IMessage {

        private UUID player;
        private boolean enabled;
        private boolean hidden;

        public ProtectionSyncMessage() {}

        ProtectionSyncMessage(UUID player, boolean enabled, boolean hidden) {
            this.player = player;
            this.enabled = enabled;
            this.hidden = hidden;
        }

        public void fromBytes(ByteBuf buffer) {
            player = new UUID(buffer.readLong(), buffer.readLong());
            enabled = buffer.readBoolean();
            hidden = buffer.readBoolean();
        }

        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(player.getMostSignificantBits());
            buffer.writeLong(player.getLeastSignificantBits());
            buffer.writeBoolean(enabled);
            buffer.writeBoolean(hidden);
        }
    }

    public static final class ProtectionSyncHandler implements IMessageHandler<ProtectionSyncMessage, IMessage> {

        public IMessage onMessage(ProtectionSyncMessage message, MessageContext context) {
            onClient(context, () -> {
                UniversalWandProtectionHandler.receiveClientProtection(message.player, message.enabled);
                UniversalWandProtectionHandler.receiveClientStealth(message.player, message.hidden);
            });
            return null;
        }
    }

    public static void requestFlightSpeed(boolean decrease) {
        NetWorkHandler.sendToServer(new FlightSpeedMessage(decrease));
    }

    public static void syncFlightSpeed(EntityPlayerMP player, float speed, boolean override, boolean notify) {
        NetWorkHandler.sendTo(new FlightSpeedSyncMessage(player.getUniqueID(), speed, override, notify), player);
    }

    public static final class FlightSpeedSyncMessage implements IMessage {

        private UUID player;
        private float speed;
        private boolean override;
        private boolean notify;

        public FlightSpeedSyncMessage() {}

        FlightSpeedSyncMessage(UUID player, float speed, boolean override, boolean notify) {
            this.player = player;
            this.speed = speed;
            this.override = override;
            this.notify = notify;
        }

        public void fromBytes(ByteBuf buffer) {
            player = new UUID(buffer.readLong(), buffer.readLong());
            speed = buffer.readFloat();
            override = buffer.readBoolean();
            notify = buffer.readBoolean();
        }

        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(player.getMostSignificantBits());
            buffer.writeLong(player.getLeastSignificantBits());
            buffer.writeFloat(speed);
            buffer.writeBoolean(override);
            buffer.writeBoolean(notify);
        }
    }

    public static final class FlightSpeedSyncHandler implements IMessageHandler<FlightSpeedSyncMessage, IMessage> {

        public IMessage onMessage(FlightSpeedSyncMessage message, MessageContext context) {
            onClient(context, () -> {
                UniversalWandUtilityHandler.receiveClientSpeed(message.player, message.speed, message.override);
                if (message.notify) UniversalWandUtilityHandler.showClientSpeedNotice();
            });
            return null;
        }
    }

    public static final class FlightSpeedMessage implements IMessage {

        private boolean decrease;

        public FlightSpeedMessage() {}

        FlightSpeedMessage(boolean decrease) {
            this.decrease = decrease;
        }

        public void fromBytes(ByteBuf buffer) {
            decrease = buffer.readBoolean();
        }

        public void toBytes(ByteBuf buffer) {
            buffer.writeBoolean(decrease);
        }
    }

    static void handleFlightSpeed(FlightSpeedMessage message, EntityPlayerMP player) {
        UniversalWandUtilityHandler.requestSpeed(player, message.decrease);
    }

    public static void requestChainKey(boolean held) {
        NetWorkHandler.sendToServer(new ChainKeyMessage(held));
    }

    public static void requestChainStart(int x, int y, int z) {
        NetWorkHandler.sendToServer(new ChainStartMessage(x, y, z));
    }

    public static final class ChainKeyMessage implements IMessage {

        private boolean held;

        public ChainKeyMessage() {}

        public ChainKeyMessage(boolean held) {
            this.held = held;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            held = buffer.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeBoolean(held);
        }
    }

    static void handleChainKey(ChainKeyMessage message, EntityPlayerMP player) {
        UniversalWandActions.request(player, UniversalWandActions.Action.CHAIN_KEY, message.held ? 1 : 0, 0, 0);
    }

    public static final class ChainStartMessage implements IMessage {

        private int x;
        private int y;
        private int z;

        public ChainStartMessage() {}

        public ChainStartMessage(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            x = buffer.readInt();
            y = buffer.readInt();
            z = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
        }
    }

    static void handleChainStart(ChainStartMessage message, EntityPlayerMP player) {

        UniversalWandMiningHandler.requestChainStart(player, message.x, message.y, message.z);
    }

    public static void requestForceBedrock(int x, int y, int z) {
        NetWorkHandler.sendToServer(new ForceBedrockMessage(x, y, z));
    }

    public static final class OpenGuiMessage implements IMessage {

        private int slot;

        public OpenGuiMessage() {}

        OpenGuiMessage(int slot) {
            this.slot = slot;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            slot = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(slot);
        }
    }

    static void handleOpenGui(OpenGuiMessage message, EntityPlayerMP player) {
        UniversalWandActions.request(player, UniversalWandActions.Action.OPEN_GUI, message.slot, 0, 0);
    }

    public static final class ForceBedrockMessage implements IMessage {

        private int x;
        private int y;
        private int z;

        public ForceBedrockMessage() {}

        public ForceBedrockMessage(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            x = buffer.readInt();
            y = buffer.readInt();
            z = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
        }
    }

    static void handleForceBedrock(ForceBedrockMessage message, EntityPlayerMP player) {
        UniversalWandActions.request(player, UniversalWandActions.Action.BEDROCK, message.x, message.y, message.z);
    }
}
