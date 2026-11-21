package com.xyp.gtneverreset.common.projector;

import java.util.function.Predicate;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;

public class ProjectorOrientation {

    public static final ForgeDirection[] PREFERRED_FRONTS = { ForgeDirection.NORTH, ForgeDirection.EAST,
        ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.UP, ForgeDirection.DOWN };
    private static final int[][] UNIT = { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };

    private ProjectorOrientation() {}

    public static ExtendedFacing canonical(Predicate<ExtendedFacing> valid) {
        for (ForgeDirection front : PREFERRED_FRONTS) {
            for (Rotation rotation : Rotation.VALUES) {
                for (Flip flip : Flip.VALUES) {
                    ExtendedFacing facing = ExtendedFacing.of(front, rotation, flip);
                    if (valid.test(facing)) return facing;
                }
            }
        }
        return ExtendedFacing.DEFAULT;
    }

    public static ExtendedFacing rotateY(ExtendedFacing facing, int quarterTurns, Predicate<ExtendedFacing> valid) {
        int turns = Math.floorMod(quarterTurns, 4);
        if (turns == 0) return facing;
        int[][] target = basis(facing);
        for (int[] axis : target) {
            for (int i = 0; i < turns; i++) {
                int x = axis[0];
                axis[0] = -axis[2];
                axis[2] = x;
            }
        }
        return find(target, facing, valid);
    }

    public static ExtendedFacing mirror(ExtendedFacing facing, Predicate<ExtendedFacing> valid) {
        int[][] target = basis(facing);
        for (int i = 0; i < 3; i++) target[0][i] = -target[0][i];
        return find(target, facing, valid);
    }

    // GT forbids vertical flips, but upside-down plus a horizontal flip draws the same shape and is allowed.
    public static ExtendedFacing flipVertical(ExtendedFacing facing, Predicate<ExtendedFacing> valid) {
        int[][] target = basis(facing);
        for (int[] axis : target) axis[1] = -axis[1];
        return find(target, facing, valid);
    }

    public static boolean mirrors(int[] m) {
        return m[0] * (m[4] * m[8] - m[5] * m[7]) - m[3] * (m[1] * m[8] - m[2] * m[7])
            + m[6] * (m[1] * m[5] - m[2] * m[4]) < 0;
    }

    public static int[] matrix(ExtendedFacing from, ExtendedFacing to) {
        int[] matrix = new int[9];
        int[] abc = new int[3], world = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            int[] unit = new int[3];
            unit[axis] = 1;
            from.getOffsetABC(unit, abc);
            to.getWorldOffset(abc, world);
            for (int row = 0; row < 3; row++) matrix[axis * 3 + row] = world[row];
        }
        return matrix;
    }

    public static ForgeDirection nextFront(ForgeDirection front) {
        return front == ForgeDirection.UP ? ForgeDirection.DOWN
            : front == ForgeDirection.DOWN ? ForgeDirection.NORTH : ForgeDirection.UP;
    }

    public static ExtendedFacing withFront(ExtendedFacing facing, ForgeDirection front,
        Predicate<ExtendedFacing> valid) {
        ExtendedFacing preferred = ExtendedFacing.of(front, facing.getRotation(), facing.getFlip());
        if (valid.test(preferred)) return preferred;
        for (ExtendedFacing candidate : ExtendedFacing.getAllWith(front)) {
            if (valid.test(candidate)) return candidate;
        }
        return facing;
    }

    public static ExtendedFacing nextRotation(ExtendedFacing facing, Predicate<ExtendedFacing> valid) {
        Rotation rotation = facing.getRotation();
        for (int i = 1; i <= Rotation.VALUES.length; i++) {
            Rotation next = Rotation.VALUES[(rotation.ordinal() + i) % Rotation.VALUES.length];
            ExtendedFacing candidate = facing.with(next);
            if (valid.test(candidate)) return candidate;
        }
        return facing;
    }

    private static ExtendedFacing find(int[][] target, ExtendedFacing fallback, Predicate<ExtendedFacing> valid) {
        for (ExtendedFacing candidate : ExtendedFacing.VALUES) {
            if (valid.test(candidate) && equal(basis(candidate), target)) return candidate;
        }
        return fallback;
    }

    private static int[][] basis(ExtendedFacing facing) {
        int[][] result = new int[3][3];
        for (int i = 0; i < 3; i++) facing.getWorldOffset(UNIT[i], result[i]);
        return result;
    }

    private static boolean equal(int[][] first, int[][] second) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (first[i][j] != second[i][j]) return false;
            }
        }
        return true;
    }
}
