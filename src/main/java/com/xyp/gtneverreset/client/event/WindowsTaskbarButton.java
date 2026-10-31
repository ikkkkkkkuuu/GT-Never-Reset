package com.xyp.gtneverreset.client.event;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.lwjgl.opengl.Display;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.config.ClientConfig;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WindowsTaskbarButton {

    private volatile boolean hidden;
    private volatile boolean running;
    private volatile long nextAttemptAt;

    /**
     * Applies a changed setting off the render thread after the game window exists.
     * A failed operation retries later without running a helper process every tick.
     *
     * @param event client tick delivered after the display may have been created
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Display.isCreated()
            || running
            || ClientConfig.hideWindowsTaskbarButton == hidden
            || System.nanoTime() < nextAttemptAt) {
            return;
        }
        if (!System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .startsWith("windows")) {
            return;
        }
        boolean desiredHidden = ClientConfig.hideWindowsTaskbarButton;
        running = true;
        Thread worker = new Thread(() -> setButtonHidden(desiredHidden), "GTNR-taskbar-button");
        worker.setDaemon(true);
        worker.start();
    }

    private void setButtonHidden(boolean desiredHidden) {
        try {
            String runtimeName = ManagementFactory.getRuntimeMXBean()
                .getName();
            String processId = runtimeName.substring(0, runtimeName.indexOf('@'));
            String script = readScript().replace("__PID__", processId)
                .replace("__VISIBLE__", desiredHidden ? "$false" : "$true");
            String command = Base64.getEncoder()
                .encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
            Process process = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-WindowStyle",
                "Hidden",
                "-EncodedCommand",
                command).redirectErrorStream(true)
                    .start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                GTNeverReset.LOG.warn("timed out changing the Windows taskbar button");
            } else if (process.exitValue() != 0) {
                GTNeverReset.LOG.warn("could not change the Windows taskbar button (exit {})", process.exitValue());
            } else {
                hidden = desiredHidden;
            }
        } catch (IOException | RuntimeException e) {
            GTNeverReset.LOG.warn("could not change the Windows taskbar button", e);
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        } finally {
            nextAttemptAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            running = false;
        }
    }

    private static String readScript() throws IOException {
        String path = "/assets/" + GTNeverReset.MODID + "/scripts/hide_taskbar_button.ps1";
        try (InputStream stream = WindowsTaskbarButton.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing taskbar script: " + path);
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
