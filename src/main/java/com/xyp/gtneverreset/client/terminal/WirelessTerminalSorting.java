package com.xyp.gtneverreset.client.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.xyp.gtneverreset.GTNeverReset;

import appeng.util.InvTweakSortingModule;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WirelessTerminalSorting {

    private static final String DEFAULT_TREE = "/assets/gtneverreset/terminal/InvTweaksTree.txt";
    private static boolean attempted;

    private WirelessTerminalSorting() {}

    public static void initialize() {
        if (attempted) return;
        attempted = true;
        try {
            // AE2 or another integration may already have supplied a custom ordering.
            if (InvTweakSortingModule.isLoaded()) return;
            Path tree = InvTweakSortingModule.CONFIG_TREE_FILE.toPath();
            if (Files.notExists(tree, LinkOption.NOFOLLOW_LINKS)) installDefault(tree);
            InvTweakSortingModule.init();
        } catch (IOException | RuntimeException | LinkageError e) {
            GTNeverReset.LOG
                .warn("could not initialize wireless terminal inventory sorting; keeping AE2's other sort modes", e);
        }
    }

    private static void installDefault(Path tree) throws IOException {
        try (InputStream input = WirelessTerminalSorting.class.getResourceAsStream(DEFAULT_TREE)) {
            if (input == null) throw new IOException("missing bundled terminal sorting tree: " + DEFAULT_TREE);
            Files.createDirectories(tree.getParent());
            Path temporary = Files.createTempFile(tree.getParent(), "gtnr-invtweaks-", ".tmp");
            try {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                try {
                    // Do not replace a user file, including one created while the resource was being copied.
                    Files.move(temporary, tree);
                    GTNeverReset.LOG.info("installed default wireless terminal sorting rules at {}", tree);
                } catch (FileAlreadyExistsException ignored) {
                    // Load the existing file instead.
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
