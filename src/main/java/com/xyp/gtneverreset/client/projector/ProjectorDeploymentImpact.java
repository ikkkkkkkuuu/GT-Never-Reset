package com.xyp.gtneverreset.client.projector;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.xyp.gtneverreset.common.projector.ProjectorDeployment;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class ProjectorDeploymentImpact {

    private static final int[] DUST_WAVE_TICKS = { 0, 6, 14 };
    private static final int SHAKE_TICKS = 16;
    static final int TAIL_TICKS = SHAKE_TICKS;
    private static final double SHAKE_RANGE = 36;

    private final World world;
    private final ProjectorDeployment bounds;
    private final long startTick;
    private int nextWave;

    ProjectorDeploymentImpact(World world, ProjectorDeployment bounds, long startTick) {
        this.world = world;
        this.bounds = bounds;
        this.startTick = startTick;
        Entity camera = Minecraft.getMinecraft().renderViewEntity;
        double x = camera == null ? bounds.centerX()
            : MathHelper.clamp_double(camera.posX, bounds.minX(), bounds.maxX());
        double z = camera == null ? bounds.centerZ()
            : MathHelper.clamp_double(camera.posZ, bounds.minZ(), bounds.maxZ());
        world.playSound(x, bounds.minY(), z, "random.anvil_land", 1.30f, 0.55f, false);
        world.playSound(x, bounds.minY(), z, "random.explode", 1.50f, 0.65f, false);
        tick(Minecraft.getMinecraft());
    }

    long age() {
        return world.getTotalWorldTime() - startTick;
    }

    void tick(Minecraft minecraft) {
        if (nextWave >= DUST_WAVE_TICKS.length) return;
        long age = age();
        Entity camera = minecraft.renderViewEntity;
        double range = (minecraft.gameSettings.renderDistanceChunks + 1) * 16.0;
        while (nextWave < DUST_WAVE_TICKS.length && age >= DUST_WAVE_TICKS[nextWave]) {
            if (camera != null && distance(camera.posX, camera.posY, camera.posZ) <= range)
                dustWave(minecraft, nextWave);
            nextWave++;
        }
    }

    void shake(Entity camera, float partial) {
        double age = age() + partial;
        if (age < 0 || age >= SHAKE_TICKS) return;
        double x = camera.prevPosX + (camera.posX - camera.prevPosX) * partial;
        double y = camera.prevPosY + (camera.posY - camera.prevPosY) * partial;
        double z = camera.prevPosZ + (camera.posZ - camera.prevPosZ) * partial;
        double strength = Math.max(0, 1 - distance(x, y, z) / SHAKE_RANGE);
        if (strength == 0) return;
        double envelope = 1 - age / SHAKE_TICKS;
        strength *= envelope * envelope;
        GL11.glRotatef((float) (2.8 * strength * Math.cos(age * 1.7)), 0, 0, 1);
        GL11.glRotatef((float) (4.5 * strength * Math.sin(age * 1.9 + 0.8)), 1, 0, 0);
        GL11.glRotatef((float) (1.5 * strength * Math.sin(age * 1.2)), 0, 1, 0);
    }

    private double distance(double x, double y, double z) {
        double dx = x - MathHelper.clamp_double(x, bounds.minX(), bounds.maxX());
        double dy = y - bounds.minY();
        double dz = z - MathHelper.clamp_double(z, bounds.minZ(), bounds.maxZ());
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void dustWave(Minecraft minecraft, int wave) {
        var random = world.rand;
        double width = bounds.maxX() - bounds.minX(), depth = bounds.maxZ() - bounds.minZ();
        int desired = MathHelper.clamp_int((int) ((width + depth) * 6), 24, 96);
        int setting = minecraft.gameSettings.particleSetting;
        int count = Math.max(1, desired / (setting == 2 ? 8 : setting == 1 ? 2 : 1));
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2 * (index + random.nextDouble()) / count;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double edge = edge(width, depth, dx, dz) + 0.2 + wave * 0.45;
            double x = bounds.centerX() + dx * edge;
            double y = bounds.minY();
            double z = bounds.centerZ() + dz * edge;
            double speed = (0.12 + random.nextDouble() * 0.16) * (1 - wave * 0.18);
            minecraft.effectRenderer.addEffect(
                new Dust(
                    world,
                    x,
                    y + 0.1 + random.nextDouble() * 0.25,
                    z,
                    dx * speed,
                    0.012 + random.nextDouble() * 0.025,
                    dz * speed));
            if (wave == 0 && index % 2 == 0) debris(minecraft, x, y, z, dx * speed, dz * speed);
        }
    }

    private void debris(Minecraft minecraft, double x, double y, double z, double vx, double vz) {
        int bx = MathHelper.floor_double(x), by = bounds.minY() - 1, bz = MathHelper.floor_double(z);
        if (by < 0 || !world.blockExists(bx, by, bz)) return;
        Block ground = world.getBlock(bx, by, bz);
        if (ground.isAir(world, bx, by, bz) || ground.getMaterial()
            .isLiquid()) return;
        var debris = new EntityDiggingFX(
            world,
            x,
            y + 0.15,
            z,
            vx,
            0.12,
            vz,
            ground,
            world.getBlockMetadata(bx, by, bz)).applyColourMultiplier(bx, by, bz);
        debris.motionX = vx;
        debris.motionY = 0.12 + world.rand.nextDouble() * 0.12;
        debris.motionZ = vz;
        minecraft.effectRenderer.addEffect(debris);
    }

    private static double edge(double width, double depth, double dx, double dz) {
        return Math.min(width / 2 / Math.max(Math.abs(dx), 0.001), depth / 2 / Math.max(Math.abs(dz), 0.001));
    }

    private static final class Dust extends EntitySmokeFX {

        private final float opacity;

        Dust(World world, double x, double y, double z, double vx, double vy, double vz) {
            super(world, x, y, z, vx, vy, vz, 2.2f + world.rand.nextFloat() * 1.4f);
            motionX = vx;
            motionY = vy;
            motionZ = vz;
            particleMaxAge = 20 + world.rand.nextInt(41);
            opacity = Math.min(0.85f, particleMaxAge * 0.015f);
            setRBGColorF(0.64f, 0.60f, 0.54f);
            setAlphaF(opacity);
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            setAlphaF(opacity * Math.max(0, 1 - (float) particleAge / particleMaxAge));
        }
    }
}
