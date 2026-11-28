package com.xyp.gtneverreset.common.packet;

import java.util.Objects;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.projector.ProjectorBuildActions;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

public final class ProjectorBuildActionPacket extends ServerboundPacket {

    private int location;
    private Action action;
    private boolean batch;

    public ProjectorBuildActionPacket() {}

    public ProjectorBuildActionPacket(int location, Action action, boolean batch) {
        this.location = location;
        this.action = Objects.requireNonNull(action);
        this.batch = batch;
    }

    @Override
    protected void read(ByteBuf buf) {
        location = buf.readInt();
        action = switch (buf.readUnsignedByte()) {
            case 0 -> Action.CANCEL;
            case 1 -> Action.UNDO;
            case 2 -> Action.DISMANTLE;
            default -> throw new IllegalArgumentException("Unknown projector build action");
        };
        batch = buf.readBoolean();
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeInt(location);
        buf.writeByte(switch (action) {
            case CANCEL -> 0;
            case UNDO -> 1;
            case DISMANTLE -> 2;
        });
        buf.writeBoolean(batch);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        ProjectorBuildActions.request(player, location, action, batch);
    }

    public enum Action {
        CANCEL,
        UNDO,
        DISMANTLE
    }
}
