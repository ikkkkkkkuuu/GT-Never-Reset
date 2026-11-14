package com.xyp.gtneverreset.common.packet;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.client.wand.UniversalWandAccelerationRenderer;
import com.xyp.gtneverreset.client.wand.UniversalWandTravelFallback;
import com.xyp.gtneverreset.common.wand.UniversalWandAccelerationHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandActions;
import com.xyp.gtneverreset.common.wand.UniversalWandFeaturePolicy;
import com.xyp.gtneverreset.common.wand.UniversalWandProtectionHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandRightClick;
import com.xyp.gtneverreset.common.wand.UniversalWandUtilityHandler;
import com.xyp.gtnrlib.network.ClientboundPacket;
import com.xyp.gtnrlib.network.ServerboundPacket;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/** Wand action and synchronization payloads. */
public final class UniversalWandNetwork {

    private UniversalWandNetwork() {}

    public static void syncFeatures(EntityPlayerMP player, long mask) {
        NetWorkHandler.sendTo(new FeatureMessage(mask), player);
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

    public static void syncAccelerationNotice(EntityPlayerMP player, int rate) {
        NetWorkHandler.sendTo(new AccelerationNoticeMessage(rate), player);
    }

    public static void requestAccelerationManagement(int action, int dimension, int x, int y, int z) {
        NetWorkHandler.sendToServer(new AccelerationManagementMessage(action, dimension, x, y, z));
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

    public static void syncProtection(EntityPlayerMP player, boolean enabled, boolean hidden) {
        ProtectionSyncMessage message = new ProtectionSyncMessage(player.getUniqueID(), enabled, hidden);
        NetWorkHandler.sendTo(message, player);
        NetWorkHandler.sendToAllAround(
            message,
            new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 512));
    }

    public static void requestFlightSpeed(boolean decrease) {
        NetWorkHandler.sendToServer(new FlightSpeedMessage(decrease));
    }

    public static void syncFlightSpeed(EntityPlayerMP player, float speed, boolean override, boolean notify) {
        NetWorkHandler.sendTo(new FlightSpeedSyncMessage(player.getUniqueID(), speed, override, notify), player);
    }

    public static void requestChainKey(boolean held) {
        NetWorkHandler.sendToServer(new ChainKeyMessage(held));
    }

    public static void requestChainStart(int x, int y, int z) {
        NetWorkHandler.sendToServer(new ChainStartMessage(x, y, z));
    }

    public static void requestForceBedrock(int x, int y, int z) {
        NetWorkHandler.sendToServer(new ForceBedrockMessage(x, y, z));
    }

    @SideOnly(Side.CLIENT)
    private static void onClient(Minecraft mc, MessageContext context, Runnable action) {
        var connection = context == null ? mc.getNetHandler() : context.netHandler;
        mc.func_152344_a(() -> { if (mc.getNetHandler() == connection) action.run(); });
    }

    public static final class FeatureMessage extends ClientboundPacket {

        private long mask;

        public FeatureMessage() {}

        FeatureMessage(long mask) {
            this.mask = mask;
        }

        @Override
        protected void read(ByteBuf buffer) {
            mask = buffer.readLong();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeLong(mask);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft) {
            handleClient(minecraft, null);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft, MessageContext context) {
            onClient(minecraft, context, () -> { UniversalWandFeaturePolicy.receive(this.mask); });
        }
    }

    public static class TravelFallbackRequest extends ServerboundPacket {

        int id;

        public TravelFallbackRequest() {}

        TravelFallbackRequest(int id) {
            this.id = id;
        }

        @Override
        protected void read(ByteBuf buffer) {
            id = buffer.readInt();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeInt(id);
        }

        @Override
        public void handleServer(EntityPlayerMP player) {
            UniversalWandRightClick.request(player, this.id);
        }
    }

    public static final class TravelFallbackGrant extends ClientboundPacket {

        int id;

        public TravelFallbackGrant() {}

        TravelFallbackGrant(int id) {
            this.id = id;
        }

        @Override
        protected void read(ByteBuf buffer) {
            id = buffer.readInt();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeInt(id);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft) {
            handleClient(minecraft, null);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft, MessageContext context) {
            onClient(minecraft, context, () -> { UniversalWandTravelFallback.allow(this.id); });
        }
    }

    public static final class TravelFallbackFinish extends TravelFallbackRequest {

        public TravelFallbackFinish() {}

        TravelFallbackFinish(int id) {
            super(id);
        }

        @Override
        public void handleServer(EntityPlayerMP player) {
            UniversalWandRightClick.finish(player, this.id);
        }
    }

    public static final class AccelerationNoticeMessage extends ClientboundPacket {

        private int rate;

        public AccelerationNoticeMessage() {}

        AccelerationNoticeMessage(int rate) {
            this.rate = rate;
        }

        @Override
        protected void read(ByteBuf buffer) {
            rate = buffer.readInt();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeInt(rate);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft) {
            handleClient(minecraft, null);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft, MessageContext context) {
            onClient(
                minecraft,
                context,
                () -> { UniversalWandUtilityHandler.showClientAccelerationNotice(this.rate); });
        }
    }

    public static final class AccelerationManagementMessage extends ServerboundPacket {

        private int action, dimension, x, y, z;

        public AccelerationManagementMessage() {}

        AccelerationManagementMessage(int action, int dimension, int x, int y, int z) {
            this.action = action;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        protected void read(ByteBuf buffer) {
            action = buffer.readInt();
            dimension = buffer.readInt();
            x = buffer.readInt();
            y = buffer.readInt();
            z = buffer.readInt();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeInt(action);
            buffer.writeInt(dimension);
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
        }

        @Override
        public void handleServer(EntityPlayerMP player) {
            if (this.action >= 0 && this.action <= 4) UniversalWandAccelerationHandler
                .requestManagement(player, this.action, this.dimension, this.x, this.y, this.z);
        }
    }

    public static final class AccelerationSyncMessage extends ClientboundPacket {

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

        @Override
        protected void read(ByteBuf buffer) {
            dimension = buffer.readInt();
            x = buffer.readInt();
            y = buffer.readInt();
            z = buffer.readInt();
            level = buffer.readInt();
            remaining = buffer.readInt();
            mode = buffer.readInt();
            throttled = buffer.readBoolean();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeInt(dimension);
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
            buffer.writeInt(level);
            buffer.writeInt(remaining);
            buffer.writeInt(mode);
            buffer.writeBoolean(throttled);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft) {
            handleClient(minecraft, null);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft, MessageContext context) {
            onClient(minecraft, context, () -> { UniversalWandAccelerationRenderer.receive(this); });
        }
    }

    public static final class ProtectionSyncMessage extends ClientboundPacket {

        private UUID player;
        private boolean enabled;
        private boolean hidden;

        public ProtectionSyncMessage() {}

        ProtectionSyncMessage(UUID player, boolean enabled, boolean hidden) {
            this.player = player;
            this.enabled = enabled;
            this.hidden = hidden;
        }

        @Override
        protected void read(ByteBuf buffer) {
            player = new UUID(buffer.readLong(), buffer.readLong());
            enabled = buffer.readBoolean();
            hidden = buffer.readBoolean();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeLong(player.getMostSignificantBits());
            buffer.writeLong(player.getLeastSignificantBits());
            buffer.writeBoolean(enabled);
            buffer.writeBoolean(hidden);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft) {
            handleClient(minecraft, null);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft, MessageContext context) {
            onClient(minecraft, context, () -> {
                UniversalWandProtectionHandler.receiveClientProtection(this.player, this.enabled);
                UniversalWandProtectionHandler.receiveClientStealth(this.player, this.hidden);
            });
        }
    }

    public static final class FlightSpeedSyncMessage extends ClientboundPacket {

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

        @Override
        protected void read(ByteBuf buffer) {
            player = new UUID(buffer.readLong(), buffer.readLong());
            speed = buffer.readFloat();
            override = buffer.readBoolean();
            notify = buffer.readBoolean();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeLong(player.getMostSignificantBits());
            buffer.writeLong(player.getLeastSignificantBits());
            buffer.writeFloat(speed);
            buffer.writeBoolean(override);
            buffer.writeBoolean(notify);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft) {
            handleClient(minecraft, null);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void handleClient(Minecraft minecraft, MessageContext context) {
            onClient(minecraft, context, () -> {
                UniversalWandUtilityHandler.receiveClientSpeed(this.player, this.speed, this.override);
                if (this.notify) UniversalWandUtilityHandler.showClientSpeedNotice();
            });
        }
    }

    public static final class FlightSpeedMessage extends ServerboundPacket {

        private boolean decrease;

        public FlightSpeedMessage() {}

        FlightSpeedMessage(boolean decrease) {
            this.decrease = decrease;
        }

        @Override
        protected void read(ByteBuf buffer) {
            decrease = buffer.readBoolean();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeBoolean(decrease);
        }

        @Override
        public void handleServer(EntityPlayerMP player) {
            UniversalWandUtilityHandler.requestSpeed(player, this.decrease);
        }
    }

    public static final class ChainKeyMessage extends ServerboundPacket {

        private boolean held;

        public ChainKeyMessage() {}

        public ChainKeyMessage(boolean held) {
            this.held = held;
        }

        @Override
        protected void read(ByteBuf buffer) {
            held = buffer.readBoolean();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeBoolean(held);
        }

        @Override
        public void handleServer(EntityPlayerMP player) {
            var handler = player.playerNetServerHandler;
            // Queue the state before the following vanilla digging packet, without another server-tick delay.
            // UniversalWandActions applies it on the server thread, including at the start of a block break.
            if (handler.netManager.isChannelOpen())
                UniversalWandActions.request(player, UniversalWandActions.Action.CHAIN_KEY, held ? 1 : 0, 0, 0);
        }
    }

    public static final class ChainStartMessage extends ServerboundPacket {

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
        protected void read(ByteBuf buffer) {
            x = buffer.readInt();
            y = buffer.readInt();
            z = buffer.readInt();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
        }

        @Override
        public void handleServer(EntityPlayerMP player) {
            // Preserve the packet ID and payload for older clients; their vanilla digging packet starts the chain.
            // A second harvest here would target the stone backfilled after that original harvest.
        }
    }

    public static final class OpenGuiMessage extends ServerboundPacket {

        private int slot;

        public OpenGuiMessage() {}

        OpenGuiMessage(int slot) {
            this.slot = slot;
        }

        @Override
        protected void read(ByteBuf buffer) {
            slot = buffer.readInt();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeInt(slot);
        }

        @Override
        public void handleServer(EntityPlayerMP player) {
            UniversalWandActions.request(player, UniversalWandActions.Action.OPEN_GUI, this.slot, 0, 0);
        }
    }

    public static final class ForceBedrockMessage extends ServerboundPacket {

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
        protected void read(ByteBuf buffer) {
            x = buffer.readInt();
            y = buffer.readInt();
            z = buffer.readInt();
        }

        @Override
        protected void write(ByteBuf buffer) {
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
        }

        @Override
        public void handleServer(EntityPlayerMP player) {
            UniversalWandActions.request(player, UniversalWandActions.Action.BEDROCK, this.x, this.y, this.z);
        }
    }

}
