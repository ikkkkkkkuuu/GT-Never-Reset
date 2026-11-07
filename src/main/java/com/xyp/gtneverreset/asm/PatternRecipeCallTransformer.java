package com.xyp.gtneverreset.asm;

import java.nio.charset.StandardCharsets;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Keeps calls through GT's processing API routed even when addons replace its implementation. */
public final class PatternRecipeCallTransformer implements IClassTransformer {

    // Forge exposes old ASM constants at compile time; UniMixins supplies ASM 9 at runtime.
    private static final int ASM_API = 9 << 16;
    private static final String LOGIC = "gregtech/api/logic/ProcessingLogic";
    private static final String ROUTING = "com/xyp/gtneverreset/common/me/PatternRecipeRouting";
    private static final String CACHE = "(Lgregtech/common/tileentities/machines/IDualInputInventoryWithPattern;)Z";
    private static final String PROCESS = "()Lgregtech/api/recipe/check/CheckRecipeResult;";
    private static final String CLEAR = "()L" + LOGIC + ";";
    private static final byte[] LOGIC_REFERENCE = LOGIC.getBytes(StandardCharsets.US_ASCII);

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || LOGIC.replace('/', '.')
            .equals(transformedName)
            || ROUTING.replace('/', '.')
                .equals(transformedName)
            || transformedName.startsWith("com.xyp.gtneverreset.mixins.")
            || !referencesLogic(bytes)) return bytes;

        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        boolean[] changed = { false };
        reader.accept(new ClassVisitor(ASM_API, writer) {

            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
                String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
                if (methodName.startsWith("gtnr$")) return visitor;
                return new MethodVisitor(ASM_API, visitor) {

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean itf) {
                        String bridge = null;
                        if (opcode == Opcodes.INVOKEVIRTUAL && LOGIC.equals(owner)) {
                            if (method.equals("tryCachePossibleRecipesFromPattern") && desc.equals(CACHE))
                                bridge = "cachePattern";
                            else if (method.equals("process") && desc.equals(PROCESS)) bridge = "processPattern";
                            else if (method.equals("clear") && desc.equals(CLEAR)) bridge = "clearProcessing";
                        }
                        if (bridge == null) {
                            super.visitMethodInsn(opcode, owner, method, desc, itf);
                        } else {
                            // Receiver becomes the first argument; stack sizes and existing frames stay valid.
                            super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                ROUTING,
                                bridge,
                                "(L" + LOGIC + ";" + desc.substring(1),
                                false);
                            changed[0] = true;
                        }
                    }
                };
            }
        }, 0);
        return changed[0] ? writer.toByteArray() : bytes;
    }

    private static boolean referencesLogic(byte[] bytes) {
        outer: for (int offset = 0; offset <= bytes.length - LOGIC_REFERENCE.length; offset++) {
            for (int index = 0; index < LOGIC_REFERENCE.length; index++) {
                if (bytes[offset + index] != LOGIC_REFERENCE[index]) continue outer;
            }
            return true;
        }
        return false;
    }
}
