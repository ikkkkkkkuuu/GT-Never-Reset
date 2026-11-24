package com.xyp.gtneverreset.common.projector;

import net.minecraft.util.MathHelper;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;

@Desugar
public record ProjectorDeployment(long startTick, int minX, int minY, int minZ, int maxX, int maxZ, int openingY) {

    public static final int START_DELAY_TICKS = 2;
    public static final int OPEN_TICKS = 16;
    public static final int DESCENT_TICKS = 92;
    public static final int IMPACT_TICK = OPEN_TICKS + DESCENT_TICKS;
    public static final int CLOSE_TICKS = 12;
    private static final double CIRCLE_MARGIN = 0.25;

    public boolean bandsAlongX() {
        return maxX - minX >= maxZ - minZ;
    }

    public int bandMin() {
        return bandsAlongX() ? minX : minZ;
    }

    public int bandMax() {
        return bandsAlongX() ? maxX : maxZ;
    }

    public int bandCount(StructureDeploymentStyle style) {
        return style.bandCount(bandMax() - bandMin());
    }

    public int bandAt(int x, int z, int count) {
        return StructureDeploymentStyle.bandAt(bandsAlongX() ? x : z, bandMin(), bandMax(), count);
    }

    public double centerX() {
        return (minX + maxX) * 0.5;
    }

    public double centerZ() {
        return (minZ + maxZ) * 0.5;
    }

    public double radius() {
        return Math.hypot(maxX - minX, maxZ - minZ) / 2 + CIRCLE_MARGIN;
    }

    public double verticalOffset(double age) {
        double progress = MathHelper.clamp_double((age - OPEN_TICKS) / DESCENT_TICKS, 0, 1);
        double descent = Math.pow(progress, 4);
        return (openingY - minY) * (1 - descent);
    }

    public float circleScale(double age) {
        double opening = MathHelper.clamp_double(age / OPEN_TICKS, 0, 1);
        double closing = MathHelper.clamp_double((age - IMPACT_TICK) / CLOSE_TICKS, 0, 1);
        return (float) ((1 - Math.pow(1 - opening, 3)) * (1 - closing * closing));
    }
}
