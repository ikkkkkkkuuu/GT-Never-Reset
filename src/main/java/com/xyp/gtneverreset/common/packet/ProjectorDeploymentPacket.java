package com.xyp.gtneverreset.common.packet;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.client.projector.ProjectorDeploymentRenderer;
import com.xyp.gtneverreset.common.projector.ProjectorDeployment;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtnrlib.network.ClientboundPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public final class ProjectorDeploymentPacket extends ClientboundPacket {

    private static final int MAX_PLAN_BYTES = 1_048_576;

    private long id;
    private int dimension;
    private ProjectorDeployment deployment;
    private NBTTagCompound plan;
    private boolean batch;
    private int finishedBands;
    private Phase phase;

    public ProjectorDeploymentPacket() {}

    public ProjectorDeploymentPacket(long id, int dimension, ProjectorDeployment deployment, ProjectorPlan plan,
        boolean batch) {
        this.id = id;
        this.dimension = dimension;
        this.deployment = deployment;
        phase = Phase.START;
        this.plan = plan.writeToNBT();
        this.batch = batch;
    }

    public ProjectorDeploymentPacket(long id, int dimension, Phase phase) {
        this.id = id;
        this.dimension = dimension;
        this.phase = phase;
    }

    public ProjectorDeploymentPacket(long id, int dimension, int finishedBands) {
        this(id, dimension, Phase.PROGRESS);
        this.finishedBands = finishedBands;
    }

    @Override
    protected void read(ByteBuf buffer) {
        id = buffer.readLong();
        dimension = buffer.readInt();
        int phaseId = buffer.readUnsignedByte();
        if (phaseId >= Phase.values().length) throw new IllegalArgumentException("Invalid projector deployment phase");
        phase = Phase.values()[phaseId];
        if (phase == Phase.PROGRESS) finishedBands = buffer.readUnsignedByte();
        if (phase != Phase.START) return;
        deployment = new ProjectorDeployment(
            buffer.readLong(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt());
        batch = buffer.readBoolean();
        int length = buffer.readInt();
        if (length <= 0 || length > MAX_PLAN_BYTES || length > buffer.readableBytes())
            throw new IllegalArgumentException("Invalid projector deployment plan length");
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        try {
            plan = CompressedStreamTools.func_152457_a(bytes, new NBTSizeTracker(2L * MAX_PLAN_BYTES));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid projector deployment plan", e);
        }
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeLong(id);
        buffer.writeInt(dimension);
        buffer.writeByte(phase.ordinal());
        if (phase == Phase.PROGRESS) buffer.writeByte(finishedBands);
        if (phase != Phase.START) return;
        buffer.writeLong(deployment.startTick());
        buffer.writeInt(deployment.minX());
        buffer.writeInt(deployment.minY());
        buffer.writeInt(deployment.minZ());
        buffer.writeInt(deployment.maxX());
        buffer.writeInt(deployment.maxZ());
        buffer.writeInt(deployment.openingY());
        buffer.writeBoolean(batch);
        try {
            byte[] bytes = CompressedStreamTools.compress(plan);
            buffer.writeInt(bytes.length);
            buffer.writeBytes(bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode projector deployment plan", e);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        ProjectorDeploymentRenderer.receive(minecraft, id, dimension, phase, deployment, plan, batch, finishedBands);
    }

    public enum Phase {
        START,
        IMPACT,
        COMPLETE,
        CANCEL,
        PROGRESS
    }
}
