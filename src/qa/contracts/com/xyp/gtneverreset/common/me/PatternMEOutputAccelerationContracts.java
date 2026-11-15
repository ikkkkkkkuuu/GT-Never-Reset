package com.xyp.gtneverreset.common.me;

import java.lang.reflect.Proxy;
import java.util.List;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.security.PlayerSource;

public final class PatternMEOutputAccelerationContracts {

    public static void main(String[] args) {
        require(!hasJob(cpu(true, null, new MachineSource(null))), "stock maintenance must not trigger acceleration");
        require(
            !hasJob(cpu(false, "player", new PlayerSource(null, null))),
            "completed player orders must not trigger");
        require(!hasJob(cpu(true, null, null)), "unknown task origin must not trigger");
        require(!hasJob(cpu(true, "", null)), "empty player name must not trigger");
        require(hasJob(cpu(true, "saved-player", null)), "player identity must survive source loss after reload");
        require(hasJob(cpu(true, null, new PlayerSource(null, null))), "player sources on custom CPUs must trigger");
        require(
            hasJob(cpu(true, null, new MachineSource(null)), cpu(true, "player", null)),
            "a mixed network must detect its player order");
        require(!hasJob(), "an idle network must not trigger");
        require(!PatternMEOutputAcceleration.isWithinHoldTime(0, 0, 100), "a new machine has never worked");
        require(!PatternMEOutputAcceleration.isWithinHoldTime(10, 20, 100), "a reset machine clock must not trigger");
        require(PatternMEOutputAcceleration.isWithinHoldTime(20, 20, 0), "zero hold permits the working tick");
        require(!PatternMEOutputAcceleration.isWithinHoldTime(21, 20, 0), "zero hold excludes the next tick");
        require(PatternMEOutputAcceleration.isWithinHoldTime(120, 20, 100), "the hold includes its boundary");
        require(!PatternMEOutputAcceleration.isWithinHoldTime(121, 20, 100), "the hold expires after its boundary");
        System.out.println("Pattern ME output acceleration contracts passed (14 checks)");
    }

    private static boolean hasJob(ICraftingCPU... cpus) {
        return PatternMEOutputAcceleration.hasPlayerJob(List.of(cpus));
    }

    private static ICraftingCPU cpu(boolean busy, String player, BaseActionSource source) {
        return (ICraftingCPU) Proxy.newProxyInstance(
            ICraftingCPU.class.getClassLoader(),
            new Class<?>[] { ICraftingCPU.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "isBusy" -> busy;
            case "getSourcePlayer" -> player;
            case "getCurrentJobSource" -> source;
            default -> throw new AssertionError("Unexpected CPU query: " + method.getName());
            });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
