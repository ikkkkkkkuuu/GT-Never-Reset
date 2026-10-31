package com.xyp.gtneverreset;

import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarFile;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class ModuleArtifactContracts {

    public static void main(String[] args) throws Exception {
        checkArchive(args[0], "gtneverreset", "com/xyp/gtnrlib/");
        checkArchive(args[1], "gtnrlib", "com/xyp/gtneverreset/");
        try (JarFile library = new JarFile(args[1])) {
            for (String type : new String[] { "client/render/icon/SpriteAtlas", "client/render/icon/FileIconCache",
                "client/render/icon/PlayerHeadCache", "client/input/KeyInjector", "client/input/KeybindStateTracker",
                "client/input/KeyModifiers", "client/input/ModifierKeys", "mixins/early/input/KeyBindingAccessor",
                "text/syntax/LuaSyntax", "client/input/KeyBindingLookup", "client/input/KeyBuffer",
                "client/input/VanillaKeyEffects", "client/render/icon/PotionIcons", "world/ChunkCoords", "world/Facing",
                "world/WorldTime", "text/TooltipLineWrapper", "utils/ImmutableCollections",
                "client/gui/ColorPickerScreen", "client/gui/ConfirmationScreen", "client/gui/TextPromptScreen",
                "client/gui/MultilineEditorScreen", "client/gui/KeyBindingPickerScreen", "gui/widget/ScrollingDropdown",
                "gui/widget/ItemStackSlot", "gui/theme/ModernGuiSkin" }) {
                require(
                    library.getEntry("com/xyp/gtnrlib/" + type + ".class") != null,
                    "Missing shared implementation: " + type);
            }
            require(library.getEntry("assets/gtnrlib/shaders/text/text.vert.glsl") != null, "library vertex shader");
            require(
                library.getEntry("assets/gtnrlib/shaders/text/exotic_rainbow.frag.glsl") != null,
                "library fragment shader");
            require(library.getEntry("META-INF/licenses/gtnrlib/SOURCE.md") != null, "library source notice");
            for (String resource : new String[] { "META-INF/licenses/gtnrlib/GPL-3.0.txt",
                "META-INF/licenses/gtnrlib/ANVILLIB-MIT.txt", "META-INF/gtnrlib_at.cfg",
                "mixins.gtnrlib.early.j17.json", "mixins.gtnrlib.early.j21.json", "mixins.gtnrlib.late.json",
                "mixins.gtnrlib.late.j17.json", "mixins.gtnrlib.late.j21.json",
                "assets/gtnrlib/textures/effects/infernum_base_rarity_glow.png",
                "assets/gtnrlib/textures/effects/infernum_gleam.png", "assets/gtnrlib/lang/en_US.lang",
                "assets/gtnrlib/lang/zh_CN.lang" })
                require(library.getEntry(resource) != null, "Missing framework resource: " + resource);
        }
        try (JarFile host = new JarFile(args[0])) {
            for (String type : new String[] { "client/radialmenu/render/SpriteAtlas",
                "client/radialmenu/render/UserIconLoader", "client/radialmenu/render/PlayerHeadIcons",
                "client/radialmenu/input/KeyInjector", "client/radialmenu/input/KeybindStateTracker",
                "client/radialmenu/input/ModifierKeys", "client/radialmenu/input/HeldKeyResync",
                "client/radialmenu/Modifiers", "client/radialmenu/script/LuaSyntax",
                "mixins/early/minecraft/KeyBindingAccessor", "loader/LifecycleLoader", "loader/EarlyMixinsLoader",
                "loader/LateMixinsLoader", "config/MainConfig", "loader/GTNRRecipeMaps",
                "utils/crafting/quantum/ae2/QuantumCraftingMode", "utils/machine/wireless/WirelessChannelManager",
                "container/ContainerQuickEncodingTerminal", "client/nei/NEIMachineConfig",
                "client/keybind/KeyBindingLookup", "client/keybind/KeyBuffer", "client/keybind/VanillaKeyEffects",
                "client/render/radialmenu/PotionIcons", "client/script/radialmenu/ChunkCoords",
                "client/script/radialmenu/Facing", "client/script/radialmenu/WorldTime",
                "utils/structure/TooltipLineWrapper", "utils/machine/wireless/WirelessCollections",
                "utils/world/InterfaceTeleportation$DirectTeleportTeleporter",
                "common/gui/WildcardPatternGui$SelectorWidget", "common/gui/WildcardPatternGui$Theme",
                "common/gui/WildcardPatternGui$PreviewPage$IconWidget" }) {
                require(
                    host.getEntry("com/xyp/gtneverreset/" + type + ".class") == null,
                    "Stale host implementation: " + type);
            }
        }
        System.out.println("Module metadata, mixin resources, bytecode and ownership contracts passed");
    }

    private static void checkArchive(String path, String modId, String forbiddenPackage) throws Exception {
        try (JarFile archive = new JarFile(path)) {
            var attributes = archive.getManifest()
                .getMainAttributes();
            String coremod = attributes.getValue("FMLCorePlugin");
            require(
                coremod != null && archive.getEntry(coremod.replace('.', '/') + ".class") != null,
                modId + " coremod entry");
            String configs = attributes.getValue("MixinConfigs");
            require(configs != null, modId + " mixin manifest");
            for (String config : configs.split(","))
                require(archive.getEntry(config.trim()) != null, modId + " missing mixin configuration: " + config);
            require(archive.getEntry("mixins." + modId + ".early.json") != null, modId + " early configuration");
            require(archive.getEntry("mixins." + modId + ".refmap.json") != null, modId + " refmap");
            var metadataEntry = archive.getEntry("mcmod.info");
            require(metadataEntry != null, modId + " metadata");
            String metadata = new String(
                archive.getInputStream(metadataEntry)
                    .readAllBytes(),
                StandardCharsets.UTF_8);
            require(metadata.contains("\"" + modId + "\"") && !metadata.contains("${"), modId + " expanded metadata");
            for (var entries = archive.entries(); entries.hasMoreElements();) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class")) continue;
                require(!name.contains(forbiddenPackage), modId + " contains other module's class: " + name);
                if (name.startsWith("META-INF/versions/")) continue;
                if (name.startsWith("com/xyp/") && name.contains("/mixins/")) {
                    try (var input = archive.getInputStream(entry)) {
                        checkAccessorMethods(new ClassReader(input), name);
                    }
                }
                try (DataInputStream input = new DataInputStream(archive.getInputStream(entry))) {
                    require(input.readInt() == 0xCAFEBABE, "class header: " + name);
                    input.readUnsignedShort();
                    require(input.readUnsignedShort() <= 52, "Java 8 base bytecode: " + name);
                }
            }
        }
    }

    private static void checkAccessorMethods(ClassReader reader, String className) {
        reader.accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {

                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        if (annotation.equals("Lorg/spongepowered/asm/mixin/gen/Accessor;")) {
                            require(
                                (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC)) != 0,
                                "Non-abstract Mixin accessor (including compiler bridges): " + className
                                    + "."
                                    + name
                                    + descriptor);
                        }
                        return null;
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
