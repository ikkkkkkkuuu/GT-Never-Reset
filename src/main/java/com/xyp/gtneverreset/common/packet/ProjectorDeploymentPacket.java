package com.xyp.gtneverreset.common.packet;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.client.projector.ProjectorDeploymentRenderer;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtnrlib.animation.StructureDeployment;
import com.xyp.gtnrlib.animation.StructureDeployment.Launch;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;
import com.xyp.gtnrlib.network.ClientboundPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public final class ProjectorDeploymentPacket extends ClientboundPacket {

    private static final int MAX_PLAN_BYTES = 1_048_576;
    private static final int LAUNCH_POSITION_BYTES = 3 * Double.BYTES;

    private long id;
    private int dimension;
    private StructureDeployment deployment;
    private Launch launch;
    private NBTTagCompound plan;
    private boolean batch;
    private int finishedBands;
    private long impactTick = -1;
    private Phase phase;

    public ProjectorDeploymentPacket() {}

    public ProjectorDeploymentPacket(long id, int dimension, StructureDeployment deployment, ProjectorPlan plan,
        boolean batch) {
        this(id, dimension, deployment, plan, batch, null);
    }

    public ProjectorDeploymentPacket(long id, int dimension, StructureDeployment deployment, ProjectorPlan plan,
        boolean batch, Launch launch) {
        this.id = id;
        this.dimension = dimension;
        this.deployment = deployment;
        this.launch = plan.buildStyle == StructureDeploymentStyle.SINGULARITY ? launch : null;
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

    public ProjectorDeploymentPacket(long id, int dimension, long impactTick) {
        this(id, dimension, Phase.IMPACT);
        this.impactTick = impactTick;
    }

    @Override
    protected void read(ByteBuf buffer) {
        id = buffer.readLong();
        dimension = buffer.readInt();
        int phaseId = buffer.readUnsignedByte();
        if (phaseId >= Phase.values().length) throw new IllegalArgumentException("Invalid projector deployment phase");
        phase = Phase.values()[phaseId];
        if (phase == Phase.PROGRESS) finishedBands = buffer.readUnsignedByte();
        // The summoning impact has no timestamp. Singularity observers use it to resume at the current age.
        impactTick = phase == Phase.IMPACT && buffer.isReadable() ? buffer.readLong() : -1;
        if (phase != Phase.START) return;
        long startTick = buffer.readLong();
        int minX = buffer.readInt(), minY = buffer.readInt(), minZ = buffer.readInt();
        int maxX = buffer.readInt(), maxZ = buffer.readInt(), openingY = buffer.readInt();
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
        launch = null;
        int extraBytes = buffer.readableBytes();
        if (extraBytes != 0) {
            if (extraBytes != LAUNCH_POSITION_BYTES && extraBytes != LAUNCH_POSITION_BYTES + Integer.BYTES)
                throw new IllegalArgumentException("Invalid projector deployment launch length");
            double x = buffer.readDouble(), y = buffer.readDouble(), z = buffer.readDouble();
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new IllegalArgumentException("Invalid projector deployment launch position");
            launch = new Launch(x, y, z);
        }
        if (buffer.isReadable()) {
            int maxY = buffer.readInt();
            if (maxY <= minY || maxY > openingY)
                throw new IllegalArgumentException("Invalid projector deployment structure height");
            deployment = new StructureDeployment(startTick, minX, minY, minZ, maxX, maxZ, openingY, maxY);
        } else {
            deployment = new StructureDeployment(startTick, minX, minY, minZ, maxX, maxZ, openingY);
        }
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeLong(id);
        buffer.writeInt(dimension);
        buffer.writeByte(phase.ordinal());
        if (phase == Phase.PROGRESS) buffer.writeByte(finishedBands);
        if (phase == Phase.IMPACT && impactTick >= 0) buffer.writeLong(impactTick);
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
        if (launch != null) {
            buffer.writeDouble(launch.x());
            buffer.writeDouble(launch.y());
            buffer.writeDouble(launch.z());
            buffer.writeInt(deployment.maxY());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        ProjectorDeploymentRenderer
            .receive(minecraft, id, dimension, phase, deployment, plan, batch, finishedBands, impactTick, launch);
    }

    public enum Phase {
        START,
        IMPACT,
        COMPLETE,
        CANCEL,
        PROGRESS
    }
}
