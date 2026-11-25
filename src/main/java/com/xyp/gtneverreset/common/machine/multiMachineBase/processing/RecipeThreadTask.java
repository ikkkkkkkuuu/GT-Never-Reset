package com.xyp.gtneverreset.common.machine.multiMachineBase.processing;

import net.minecraft.nbt.NBTTagCompound;

import gregtech.api.util.OverclockCalculator;

public final class RecipeThreadTask {

    private final int thread;
    private final int parallels;
    private final RecipeThreadOutputs outputs;
    private long eut;
    private int duration;
    private int progress;
    private OverclockSettings overclock;
    private int fullDurationAtCurrentRate;
    private long launchEUt;
    private int launchDuration;

    public RecipeThreadTask(int thread, long eut, int duration, int parallels, RecipeThreadOutputs outputs) {
        if (thread < 0 || eut < 0 || duration < 1 || parallels < 1 || outputs == null) {
            throw new IllegalArgumentException("Invalid recipe thread task");
        }
        this.thread = thread;
        this.eut = eut;
        this.duration = duration;
        this.parallels = parallels;
        this.outputs = outputs;
    }

    public RecipeThreadTask(int thread, long eut, int duration, int parallels, RecipeThreadOutputs outputs,
        int progress) {
        this(thread, eut, duration, parallels, outputs);
        this.progress = Math.max(0, Math.min(duration, progress));
    }

    RecipeThreadTask(int thread, long eut, int duration, int parallels, RecipeThreadOutputs outputs,
        OverclockSettings overclock) {
        this(thread, eut, duration, parallels, outputs);
        this.overclock = overclock;
        fullDurationAtCurrentRate = duration;
        launchEUt = eut;
        launchDuration = duration;
    }

    public int getThread() {
        return thread;
    }

    public long getEUt() {
        return eut;
    }

    public int getDuration() {
        return duration;
    }

    public int getParallels() {
        return parallels;
    }

    public RecipeThreadOutputs getOutputs() {
        return outputs;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isFinished() {
        return progress >= duration;
    }

    public void advance() {
        if (!isFinished()) progress++;
    }

    public boolean accelerate(long voltageLimit, long sparePower) {
        int remaining = duration - progress;
        if (overclock == null || remaining <= 1 || sparePower <= 0 || voltageLimit <= 0) return false;
        long budget = eut + Math.min(sparePower, Long.MAX_VALUE - eut);
        var calculator = overclock.calculate(Math.min(voltageLimit, budget), budget, parallels);
        int fasterDuration = calculator.getDuration();
        long fasterEUt = calculator.getConsumption();
        if (fasterDuration >= fullDurationAtCurrentRate || fasterEUt < eut
            || fasterEUt > budget
            || fasterEUt == Long.MAX_VALUE) return false;

        long fasterRemaining = remainingAtRate(fasterDuration);
        if (fasterRemaining >= remaining) return false;
        setRate(fasterEUt, fasterDuration, fasterRemaining);
        return true;
    }

    public boolean restoreLaunchRate() {
        if (overclock == null || eut <= launchEUt || isFinished()) return false;
        setRate(launchEUt, launchDuration, remainingAtRate(launchDuration));
        return true;
    }

    private long remainingAtRate(int fullDuration) {
        // Preserve work already done, rounding the remaining ticks up when changing rates.
        return ((long) (duration - progress) * fullDuration + fullDurationAtCurrentRate - 1)
            / fullDurationAtCurrentRate;
    }

    private void setRate(long power, int fullDuration, long remaining) {
        eut = power;
        duration = (int) Math.min(Integer.MAX_VALUE, progress + remaining);
        fullDurationAtCurrentRate = fullDuration;
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("thread", thread);
        tag.setLong("eut", eut);
        tag.setInteger("duration", duration);
        tag.setInteger("progress", progress);
        tag.setInteger("parallels", parallels);
        tag.setTag("outputs", outputs.save());
        if (overclock != null) {
            NBTTagCompound settings = overclock.save();
            settings.setInteger("rateDuration", fullDurationAtCurrentRate);
            settings.setLong("launchEUt", launchEUt);
            settings.setInteger("launchDuration", launchDuration);
            tag.setTag("overclock", settings);
        }
        return tag;
    }

    public static RecipeThreadTask load(NBTTagCompound tag) {
        RecipeThreadTask task = new RecipeThreadTask(
            tag.getInteger("thread"),
            tag.getLong("eut"),
            tag.getInteger("duration"),
            tag.getInteger("parallels"),
            RecipeThreadOutputs.load(tag.getCompoundTag("outputs")),
            tag.getInteger("progress"));
        if (tag.hasKey("overclock", 10)) {
            NBTTagCompound settings = tag.getCompoundTag("overclock");
            int rateDuration = settings.getInteger("rateDuration");
            long launchEUt = settings.getLong("launchEUt");
            int launchDuration = settings.getInteger("launchDuration");
            if (rateDuration > 0 && rateDuration <= task.duration
                && launchEUt > 0
                && launchEUt <= task.eut
                && launchDuration >= rateDuration) {
                task.overclock = OverclockSettings.load(settings);
                task.fullDurationAtCurrentRate = rateDuration;
                task.launchEUt = launchEUt;
                task.launchDuration = launchDuration;
            }
        }
        return task;
    }

    public record OverclockSettings(int recipeEUt, int recipeDuration, double euModifier, double durationModifier,
        double powerIncrease, double timeReduction) {

        private OverclockCalculator calculate(long voltage, long budget, int parallels) {
            return new OverclockCalculator().setEUt(voltage)
                .setAmperage(budget / voltage)
                .setAmperageOC(false)
                .setRecipeEUt(recipeEUt)
                .setDuration(recipeDuration)
                .setEUtDiscount(euModifier)
                .setDurationModifier(durationModifier)
                .setEUtIncreasePerOC(powerIncrease)
                .setDurationDecreasePerOC(timeReduction)
                .setParallel(parallels)
                .setCurrentParallel(parallels)
                .calculate();
        }

        private NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("eut", recipeEUt);
            tag.setInteger("duration", recipeDuration);
            tag.setDouble("euModifier", euModifier);
            tag.setDouble("durationModifier", durationModifier);
            tag.setDouble("powerIncrease", powerIncrease);
            tag.setDouble("timeReduction", timeReduction);
            return tag;
        }

        private static OverclockSettings load(NBTTagCompound tag) {
            int eut = tag.getInteger("eut"), duration = tag.getInteger("duration");
            double euModifier = tag.getDouble("euModifier"), durationModifier = tag.getDouble("durationModifier");
            double powerIncrease = tag.getDouble("powerIncrease"), timeReduction = tag.getDouble("timeReduction");
            if (eut <= 0 || duration <= 0
                || !Double.isFinite(euModifier)
                || euModifier <= 0
                || !Double.isFinite(durationModifier)
                || durationModifier <= 0
                || !Double.isFinite(powerIncrease)
                || powerIncrease < 1
                || !Double.isFinite(timeReduction)
                || timeReduction <= 1) return null;
            return new OverclockSettings(eut, duration, euModifier, durationModifier, powerIncrease, timeReduction);
        }
    }
}
