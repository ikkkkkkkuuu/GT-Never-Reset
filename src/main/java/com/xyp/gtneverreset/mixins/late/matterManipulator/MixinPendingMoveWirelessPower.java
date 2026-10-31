package com.xyp.gtneverreset.mixins.late.matterManipulator;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.recursive_pineapple.matter_manipulator.common.building.BlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.PendingMove;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.xyp.gtneverreset.common.wireless.WirelessPowerSavedData;

@Mixin(value = PendingMove.class, remap = false)
public abstract class MixinPendingMoveWirelessPower {

    @WrapMethod(method = "moveBlock")
    private static boolean gtnr$movePower(PendingMove move, World world, Location source, BlockSpec sourceSpec,
        Location destination, BlockSpec destinationSpec, Operation<Boolean> original) {
        if (world.isRemote) return original.call(move, world, source, sourceSpec, destination, destinationSpec);
        WirelessPowerSavedData data = WirelessPowerSavedData.get();
        var sourceConnection = data == null ? null : data.connectionAt(gtnr$loadedTile(world, source));
        var destinationConnection = data == null ? null : data.connectionAt(gtnr$loadedTile(world, destination));
        boolean moved = original.call(move, world, source, sourceSpec, destination, destinationSpec);
        if (moved && data != null) {
            data.relocate(sourceConnection, gtnr$loadedTile(world, destination));
            data.relocate(destinationConnection, gtnr$loadedTile(world, source));
        }
        return moved;
    }

    @Unique
    private static TileEntity gtnr$loadedTile(World world, Location location) {
        World target = world.provider.dimensionId == location.worldId ? world
            : DimensionManager.getWorld(location.worldId);
        return target == null || !target.getChunkProvider()
            .chunkExists(location.x >> 4, location.z >> 4) ? null
                : target.getTileEntity(location.x, location.y, location.z);
    }
}
