package com.xyp.gtneverreset.common.crafting.chaos.result;

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;

import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosMachineConfiguration;

import gregtech.api.recipe.check.CheckRecipeResult;

/** Sync the missing types, so the receiving client can translate their names. */
public final class BottledChaosMissingCategoriesResult implements CheckRecipeResult {

    private BigInteger missing;
    private boolean running;

    public BottledChaosMissingCategoriesResult(long missing) {
        this(new BigInteger(Long.toUnsignedString(missing)), false);
    }

    public BottledChaosMissingCategoriesResult(BigInteger missing, boolean running) {
        this.missing = missing;
        this.running = running;
    }

    @Override
    public @NotNull String getID() {
        return "gtnr_bottled_chaos_missing_categories";
    }

    @Override
    public boolean wasSuccessful() {
        return false;
    }

    @Override
    public @NotNull String getDisplayString() {
        BigInteger displayMissing = BigInteger.ZERO;
        for (int slot = 0; slot < BottledChaosMachineConfiguration.poolCount(); slot++) {
            if (!missing.testBit(slot)) continue;
            displayMissing = displayMissing
                .setBit(running ? slot : BottledChaosMachineConfiguration.displayPoolSlot(slot));
        }
        Set<String> names = new LinkedHashSet<>();
        for (int slot = 0; slot < BottledChaosMachineConfiguration.poolCount(); slot++) {
            if (displayMissing.testBit(slot)) names.add(
                running ? BottledChaosMachineConfiguration.sourceName(slot)
                    : BottledChaosMachineConfiguration.poolName(slot));
        }
        return StatCollector.translateToLocalFormatted(
            // #tr gtnr.machine.BottleInChaos.result.missing_recipe_types
            // # Missing machines for recipe pools: %s
            // # zh_CN 缺少配方池对应的机器: %s
            "gtnr.machine.BottleInChaos.result.missing_recipe_types",
            String.join(", ", names));
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound tag) {
        tag.setByteArray("missingPools", missing.toByteArray());
        tag.setBoolean("running", running);
        return tag;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound tag) {
        byte[] bytes = tag.getByteArray("missingPools");
        missing = bytes.length > 0 ? new BigInteger(1, bytes)
            : new BigInteger(Long.toUnsignedString(tag.getLong("missing")));
        running = tag.getBoolean("running");
    }

    @Override
    public @NotNull CheckRecipeResult newInstance() {
        return new BottledChaosMissingCategoriesResult(0);
    }

    @Override
    public void encode(@NotNull PacketBuffer buffer) {
        byte[] bytes = missing.toByteArray();
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
        buffer.writeBoolean(running);
    }

    @Override
    public void decode(@NotNull PacketBuffer buffer) {
        int length = buffer.readInt();
        if (length < 1 || length > 1 + BottledChaosMachineConfiguration.MAX_POOLS / 8)
            throw new IllegalArgumentException("Invalid recipe pool mask length");
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        missing = new BigInteger(1, bytes);
        running = buffer.readBoolean();
    }

    @Override
    public boolean persistsOnShutdown() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BottledChaosMissingCategoriesResult result && result.missing.equals(missing)
            && result.running == running;
    }

    @Override
    public int hashCode() {
        return 31 * missing.hashCode() + Boolean.hashCode(running);
    }
}
