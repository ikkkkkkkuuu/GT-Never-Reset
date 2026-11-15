package com.xyp.gtneverreset.common.crafting.chaos;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

/** Persistent execution state, independent of the controller's transient hatch discovery. */
public final class ChaosProcessingGroup {

    private NBTTagCompound credential;
    private ChaosChain chain;
    private ChaosBuffer cache = new ChaosBuffer();
    private ChaosBuffer pending = new ChaosBuffer();
    private RecipeThreadOutputs delivery;
    private int step, iteration, remaining, duration;
    private long eut;
    private boolean enabled = true, exporting;
    private String status = "gtnr.chaos.status.idle";

    public void bind(NBTTagCompound credential) {
        if (busy()) throw new IllegalStateException("processing group busy");
        chain = ChaosChain.load(credential);
        this.credential = (NBTTagCompound) credential.copy();
        step = iteration = 0;
    }

    public ChaosChain.Step currentStep() {
        if (credential == null || exporting) return null;
        if (chain == null) chain = ChaosChain.load(credential);
        return step < chain.steps()
            .size() ? chain.steps()
                .get(step) : null;
    }

    public void begin(ChaosBuffer afterInputs, ChaosBuffer outputs, long eut, int duration) {
        if (remaining > 0 || eut <= 0 || duration <= 0) throw new IllegalStateException("invalid group start");
        cache = afterInputs;
        pending = outputs;
        this.eut = eut;
        this.duration = remaining = duration;
        status = "gtnr.chaos.status.running";
    }

    public void advance() {
        if (remaining <= 0) return;
        if (--remaining != 0) return;
        cache.add(pending);
        pending = new ChaosBuffer();
        eut = 0;
        if (exporting) {
            enabled = false;
            return;
        }
        ChaosChain.Step finished = currentStep();
        if (finished != null && ++iteration >= finished.repeats()) {
            iteration = 0;
            step++;
        }
    }

    public boolean flush(MTEMultiBlockBase controller) {
        if (remaining > 0) return false;
        if (!exporting && currentStep() != null) return false;
        if (delivery != null) {
            if (!delivery.flush(controller)) return false;
            delivery = null;
        }
        if (!cache.isEmpty()) {
            ChaosBuffer.View slice = cache.view();
            delivery = new RecipeThreadOutputs(slice.items, slice.fluids);
            cache.removeSlice(slice);
            controller.markDirty();
            if (!delivery.flush(controller)) return false;
            delivery = null;
        }
        if (!cache.isEmpty()) return false;
        credential = null;
        chain = null;
        step = iteration = duration = 0;
        exporting = false;
        status = "gtnr.chaos.status.idle";
        return true;
    }

    public boolean requestExport() {
        if (!busy()) {
            enabled = false;
            exporting = false;
            return true;
        }
        if (remaining == 0) enabled = false;
        exporting = true;
        return true;
    }

    public boolean busy() {
        return credential != null || remaining > 0 || !cache.isEmpty() || delivery != null;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean exporting() {
        return exporting;
    }

    public ChaosBuffer cache() {
        return cache;
    }

    public int remaining() {
        return remaining;
    }

    public int duration() {
        return duration;
    }

    public int stepIndex() {
        return step;
    }

    public long eut() {
        return remaining > 0 ? eut : 0;
    }

    public String status() {
        return status;
    }

    public void status(String key) {
        status = key;
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        if (credential != null) tag.setTag("credential", credential.copy());
        tag.setTag("cache", cache.save());
        tag.setTag("pending", pending.save());
        if (delivery != null) tag.setTag("delivery", delivery.save());
        tag.setInteger("step", step);
        tag.setInteger("iteration", iteration);
        tag.setInteger("remaining", remaining);
        tag.setInteger("duration", duration);
        tag.setLong("eut", eut);
        tag.setBoolean("enabled", enabled);
        tag.setBoolean("exporting", exporting);
        return tag;
    }

    public static ChaosProcessingGroup load(NBTTagCompound tag) {
        ChaosProcessingGroup group = new ChaosProcessingGroup();
        if (tag.hasKey("credential")) group.credential = (NBTTagCompound) tag.getCompoundTag("credential")
            .copy();
        group.cache = ChaosBuffer.load(tag.getCompoundTag("cache"));
        group.pending = ChaosBuffer.load(tag.getCompoundTag("pending"));
        if (tag.hasKey("delivery")) group.delivery = RecipeThreadOutputs.load(tag.getCompoundTag("delivery"));
        group.step = tag.getInteger("step");
        group.iteration = tag.getInteger("iteration");
        group.remaining = tag.getInteger("remaining");
        group.duration = tag.getInteger("duration");
        group.eut = tag.getLong("eut");
        group.enabled = !tag.hasKey("enabled") || tag.getBoolean("enabled");
        group.exporting = tag.getBoolean("exporting");
        if (group.step < 0 || group.step > ChaosChain.MAX_STEPS
            || group.iteration < 0
            || group.iteration > 4096
            || group.remaining < 0
            || group.remaining > group.duration
            || group.eut < 0) throw new IllegalArgumentException("invalid processing group state");
        return group;
    }
}
