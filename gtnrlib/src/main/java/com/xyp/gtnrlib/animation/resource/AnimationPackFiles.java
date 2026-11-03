package com.xyp.gtnrlib.animation.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Discovers only requested animation directories. Archive contents are never extracted. */
public final class AnimationPackFiles {

    private static final int MAX_FILE = 2 * 1024 * 1024;
    private static final int MAX_TOTAL = 32 * 1024 * 1024;
    private static final int MAX_COUNT = 4096;
    private final List<String> roots;
    private final BiConsumer<String, String> reader;
    private int bytes, count;

    private AnimationPackFiles(List<String> roots, BiConsumer<String, String> reader) {
        this.roots = roots;
        this.reader = reader;
    }

    public static void read(Path source, List<String> roots, BiConsumer<String, String> reader) throws IOException {
        var scan = new AnimationPackFiles(List.copyOf(roots), reader);
        if (Files.isDirectory(source)) scan.folder(source.toRealPath());
        else scan.archive(source);
    }

    private void folder(Path root) throws IOException {
        for (String prefix : roots) {
            Path dir = root.resolve(prefix)
                .normalize();
            if (!dir.startsWith(root) || !Files.isDirectory(dir)
                || !dir.toRealPath()
                    .startsWith(root))
                continue;
            try (var files = Files.walk(dir, 32)) {
                List<Path> found = files.filter(
                    p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) && p.toString()
                        .endsWith(".json"))
                    .limit(MAX_COUNT + 1)
                    .sorted()
                    .toList();
                if (found.size() > MAX_COUNT) throw new IOException("too many animation files");
                for (Path p : found) {
                    if (!p.toRealPath()
                        .startsWith(root)) throw new IOException("animation file outside pack");
                    try (var in = Files.newInputStream(p)) {
                        readFile(
                            root.relativize(p)
                                .toString()
                                .replace('\\', '/'),
                            in);
                    }
                }
            }
        }
    }

    private void archive(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            var entries = zip.entries();
            List<ZipEntry> found = new ArrayList<>();
            while (entries.hasMoreElements()) {
                var e = entries.nextElement();
                String n = e.getName();
                if (e.isDirectory() || !n.endsWith(".json")
                    || roots.stream()
                        .noneMatch(n::startsWith))
                    continue;
                if (n.contains("\\") || n.contains("/../") || n.contains("/./") || n.contains("//"))
                    throw new IOException("invalid archive path: " + n);
                if (found.size() >= MAX_COUNT) throw new IOException("too many animation files");
                found.add(e);
            }
            found.sort(Comparator.comparing(ZipEntry::getName));
            for (var e : found) try (var in = zip.getInputStream(e)) {
                readFile(e.getName(), in);
            }
        }
    }

    private void readFile(String path, InputStream in) throws IOException {
        byte[] content = in.readNBytes(MAX_FILE + 1);
        bytes += content.length;
        if (++count > MAX_COUNT || content.length > MAX_FILE || bytes > MAX_TOTAL)
            throw new IOException("animation pack limit exceeded at " + path);
        reader.accept(path, new String(content, StandardCharsets.UTF_8));
    }
}
