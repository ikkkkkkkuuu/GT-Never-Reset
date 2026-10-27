package com.xyp.gtneverreset.mixins.early.gtnhlib;

import java.util.Queue;
import java.util.concurrent.FutureTask;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = ServerThreadUtil.class, remap = false)
public abstract class ServerThreadUtilMixin {

    @Shadow
    @Final
    private static Queue<FutureTask<?>> futureTaskQueue;

    @Shadow
    private static Thread serverThread;

    @WrapMethod(method = "setup")
    private static void gtnr$replaceServer(MinecraftServer server, Thread thread, Operation<Void> original) {
        synchronized (futureTaskQueue) {
            // An integrated server can start before the previous server's run() returns.
            if (serverThread != thread) futureTaskQueue.clear();
            original.call(server, thread);
        }
    }

    @WrapMethod(method = "clear")
    private static void gtnr$clearOwnServer(Operation<Void> original) {
        synchronized (futureTaskQueue) {
            // A retiring server must not clear the new server's reference or queued tasks.
            if (serverThread == null || serverThread == Thread.currentThread()) original.call();
        }
    }
}
