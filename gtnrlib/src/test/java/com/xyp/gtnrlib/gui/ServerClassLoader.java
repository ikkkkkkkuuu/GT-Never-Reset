package com.xyp.gtnrlib.gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Loads the test classpath the way a dedicated server sees it: client-only packages are missing, classes marked
 * {@code @SideOnly(Side.CLIENT)} refuse to load, and members marked that way are stripped, as FML's side transformer
 * does.
 */
public class ServerClassLoader extends URLClassLoader {

    private static final String SIDE_ONLY = "Lcpw/mods/fml/relauncher/SideOnly;";
    private static final String[] MISSING = { "net.minecraft.client.", "net.minecraftforge.client.",
        "cpw.mods.fml.client.", "org.lwjgl." };
    private static final String[] SHARED = { "java.", "javax.", "jdk.", "sun.", "com.sun.", "org.w3c.", "org.xml.",
        "org.junit.", "org.opentest4j." };

    public ServerClassLoader() {
        super(classpath(), ServerClassLoader.class.getClassLoader());
    }

    // Child first: the build downgrades this test to Java 8, where there is no platform loader to sit between.
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) loaded = shared(name) ? getParent().loadClass(name) : findClass(name);
            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }

    private static boolean shared(String name) {
        for (String prefix : SHARED) if (name.startsWith(prefix)) return true;
        return false;
    }

    private static URL[] classpath() {
        List<URL> urls = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path")
            .split(File.pathSeparator)) {
            try {
                urls.add(
                    new File(entry).toURI()
                        .toURL());
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Bad classpath entry " + entry, e);
            }
        }
        return urls.toArray(new URL[0]);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (String prefix : MISSING) {
            if (name.startsWith(prefix)) throw new ClassNotFoundException("Client-only class on a server: " + name);
        }
        byte[] bytes;
        try (InputStream in = getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (in == null) throw new ClassNotFoundException(name);
            bytes = serverSide(name, in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    private static byte[] serverSide(String name, byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.SKIP_CODE);
        if (clientOnly(node.visibleAnnotations))
            throw new IllegalStateException("Attempted to load class " + name + " for invalid side SERVER");
        Set<String> stripped = new HashSet<>();
        for (MethodNode method : node.methods)
            if (clientOnly(method.visibleAnnotations)) stripped.add(method.name + method.desc);
        for (FieldNode field : node.fields) if (clientOnly(field.visibleAnnotations)) stripped.add(field.name);
        if (stripped.isEmpty()) return bytes;
        // Copying through a writer built on the reader keeps the original constant pool and untouched method bodies,
        // which older ASM versions on this classpath would otherwise re-encode wrongly.
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {

            @Override
            public MethodVisitor visitMethod(int access, String method, String desc, String signature,
                String[] exceptions) {
                return stripped.contains(method + desc) ? null
                    : super.visitMethod(access, method, desc, signature, exceptions);
            }

            @Override
            public FieldVisitor visitField(int access, String field, String desc, String signature, Object value) {
                return stripped.contains(field) ? null : super.visitField(access, field, desc, signature, value);
            }
        }, 0);
        return writer.toByteArray();
    }

    private static boolean clientOnly(List<AnnotationNode> annotations) {
        if (annotations == null) return false;
        for (AnnotationNode annotation : annotations) {
            if (!SIDE_ONLY.equals(annotation.desc) || annotation.values == null) continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if (annotation.values.get(i + 1) instanceof String[]side && "CLIENT".equals(side[1])) return true;
            }
        }
        return false;
    }
}
