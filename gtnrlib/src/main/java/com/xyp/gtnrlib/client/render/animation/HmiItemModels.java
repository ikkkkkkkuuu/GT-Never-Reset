package com.xyp.gtnrlib.client.render.animation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xyp.gtnrlib.compat.hmi.HmiPoseProgram;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** First-person model variants from the local HMI release, indexed by the identity used by its scripts. */
@SideOnly(Side.CLIENT)
public final class HmiItemModels implements AutoCloseable {

    private final Map<String, Mesh> base = new HashMap<>();
    private final NavigableMap<Float, Mesh> drinking = new TreeMap<>();
    private final Map<String, Map<String, Mesh>> variants = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();

    public HmiItemModels(Map<String, byte[]> assets) throws IOException {
        for (String path : assets.keySet()) {
            if (path.startsWith("textures/particle/") && path.endsWith(".png"))
                textures.put(path, new Texture(assets, path));
        }
        Map<String, Mesh> meshes = new HashMap<>();
        for (String path : assets.keySet()) {
            if (!path.startsWith("items/") || !path.endsWith(".json")) continue;
            String id = "minecraft:" + path.substring("items/".length(), path.length() - ".json".length());
            JsonObject select = json(assets, path).getAsJsonObject("model");
            JsonObject model = select.getAsJsonObject("fallback");
            for (JsonElement entry : select.getAsJsonArray("cases")) {
                var rule = entry.getAsJsonObject();
                for (JsonElement context : rule.getAsJsonArray("when")) {
                    if (context.getAsString()
                        .equals("thirdperson_righthand")) model = rule.getAsJsonObject("model");
                }
            }
            if (model.has("on_true")) {
                for (JsonElement threshold : model.getAsJsonObject("on_true")
                    .getAsJsonArray("entries")) {
                    JsonObject t = threshold.getAsJsonObject();
                    drinking.put(
                        t.get("threshold")
                            .getAsFloat(),
                        mesh(assets, meshes, t.getAsJsonObject("model")));
                }
                model = model.getAsJsonObject("on_false");
            } else if (model.has("component")) {
                var choices = new HashMap<String, Mesh>();
                for (JsonElement choice : model.getAsJsonArray("cases")) {
                    var rule = choice.getAsJsonObject();
                    choices.put(
                        rule.get("when")
                            .getAsString(),
                        mesh(assets, meshes, rule.getAsJsonObject("model")));
                }
                variants.put(id, choices);
                model = model.getAsJsonObject("fallback");
            }
            base.put(id, mesh(assets, meshes, model));
        }
    }

    private Mesh mesh(Map<String, byte[]> assets, Map<String, Mesh> cache, JsonObject reference) throws IOException {
        String path = reference.get("model")
            .getAsString()
            .replace("minecraft:", "");
        if (cache.containsKey(path)) return cache.get(path);
        JsonObject json = json(assets, "models/" + path + ".json");
        JsonObject display = json.getAsJsonObject("display")
            .getAsJsonObject("thirdperson_righthand");
        Vector3f t = vector(display, "translation").div(16),
            r = display.has("rotation") ? vector(display, "rotation").mul((float) Math.PI / 180) : new Vector3f(),
            s = vector(display, "scale");
        Matrix4f mount = new Matrix4f().translation(t)
            .rotateXYZ(r.x, r.y, r.z)
            .scale(s)
            .translate(-.5f, -.5f, -.5f);
        List<Face> faces = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("elements")) {
            var box = element.getAsJsonObject();
            Vector3f from = vector(box, "from").div(16), to = vector(box, "to").div(16);
            Matrix4f orient = new Matrix4f();
            if (box.has("rotation")) {
                var rot = box.getAsJsonObject("rotation");
                var pivot = vector(rot, "origin").div(16);
                float angle = (float) Math.toRadians(
                    rot.get("angle")
                        .getAsDouble());
                orient.translate(pivot);
                switch (rot.get("axis")
                    .getAsString()) {
                    case "x" -> orient.rotateX(angle);
                    case "y" -> orient.rotateY(angle);
                    case "z" -> orient.rotateZ(angle);
                }
                orient.translate(-pivot.x, -pivot.y, -pivot.z);
            }
            for (SideFace side : SideFace.values()) {
                var surface = box.getAsJsonObject("faces")
                    .getAsJsonObject(
                        side.name()
                            .toLowerCase(Locale.ROOT));
                if (surface == null) continue;
                String tex = json.getAsJsonObject("textures")
                    .get(
                        surface.get("texture")
                            .getAsString()
                            .substring(1))
                    .getAsString();
                if (!textures.containsKey(tex)) textures.put(tex, new Texture(assets, "textures/" + tex + ".png"));
                var uv = surface.getAsJsonArray("uv");
                int rotation = surface.has("rotation") ? surface.get("rotation")
                    .getAsInt() / 90 : 0;
                float[] points = new float[20];
                for (int i = 0; i < 4; i++) {
                    int bits = side.corners[i];
                    var v = new Vector3f(
                        (bits & 4) == 0 ? from.x : to.x,
                        (bits & 2) == 0 ? from.y : to.y,
                        (bits & 1) == 0 ? from.z : to.z);
                    orient.transformPosition(v);
                    int corner = (i + rotation) % 4, start = i * 5;
                    points[start] = v.x;
                    points[start + 1] = v.y;
                    points[start + 2] = v.z;
                    points[start + 3] = uv.get(corner < 2 ? 0 : 2)
                        .getAsFloat() / 16;
                    points[start + 4] = uv.get(corner == 0 || corner == 3 ? 1 : 3)
                        .getAsFloat() / 16;
                }
                faces.add(
                    new Face(
                        textures.get(tex),
                        points,
                        orient.transformDirection(new Vector3f(side.nx, side.ny, side.nz))));
            }
        }
        Mesh result = new Mesh(mount, faces);
        cache.put(path, result);
        return result;
    }

    public boolean draw(String id, String variant, int ticksUsed, int age, HmiPoseProgram program) {
        var mesh = base.get(id);
        var choices = variants.get(id);
        if (choices != null) mesh = choices.getOrDefault(variant, mesh);
        if (mesh == null) return false;
        if (id.equals("minecraft:milk_bucket") && ticksUsed >= 0) {
            var stage = drinking.floorEntry((float) ticksUsed);
            if (stage != null) mesh = stage.getValue();
        }
        HmiPoseRenderer.apply(mesh.mount);
        var tess = Tessellator.instance;
        for (int i = 0; i < mesh.faces.size(); i++) {
            Face face = mesh.faces.get(i);
            face.texture.bind(age);
            GL11.glPushMatrix();
            try {
                Matrix4f pose = program.part(i);
                if (pose != null) HmiPoseRenderer.apply(pose);
                tess.startDrawingQuads();
                tess.setNormal(face.normal.x, face.normal.y, face.normal.z);
                for (int j = 0; j < 20; j += 5) tess.addVertexWithUV(
                    face.vertices[j],
                    face.vertices[j + 1],
                    face.vertices[j + 2],
                    face.vertices[j + 3],
                    face.vertices[j + 4]);
                tess.draw();
            } finally {
                GL11.glPopMatrix();
            }
        }
        return true;
    }

    boolean bindParticle(String texture) {
        var sprite = textures.get(texture);
        if (sprite == null) return false;
        sprite.bind(0);
        return true;
    }

    @Override
    public void close() {
        for (var tex : textures.values()) if (tex.gpu != null) tex.gpu.deleteGlTexture();
    }

    private static JsonObject json(Map<String, byte[]> assets, String path) {
        return new JsonParser().parse(new String(assets.get(path), StandardCharsets.UTF_8))
            .getAsJsonObject();
    }

    private static Vector3f vector(JsonObject obj, String key) {
        var a = obj.getAsJsonArray(key);
        return new Vector3f(
            a.get(0)
                .getAsFloat(),
            a.get(1)
                .getAsFloat(),
            a.get(2)
                .getAsFloat());
    }

    private record Face(Texture texture, float[] vertices, Vector3f normal) {}

    private record Mesh(Matrix4f mount, List<Face> faces) {}

    private enum SideFace {

        DOWN(0, -1, 0, 1, 0, 4, 5),
        UP(0, 1, 0, 2, 3, 7, 6),
        NORTH(0, 0, -1, 6, 4, 0, 2),
        SOUTH(0, 0, 1, 3, 1, 5, 7),
        WEST(-1, 0, 0, 2, 0, 1, 3),
        EAST(1, 0, 0, 7, 5, 4, 6);

        final int nx, ny, nz;
        final int[] corners;

        SideFace(int x, int y, int z, int... corners) {
            nx = x;
            ny = y;
            nz = z;
            this.corners = corners;
        }
    }

    private static final class Texture {

        final int[] pixels, frames;
        final int size, frameTime;
        final boolean interpolate;
        DynamicTexture gpu;
        int last = -1;

        Texture(Map<String, byte[]> assets, String path) throws IOException {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(assets.get(path)));
            size = image.getWidth();
            pixels = image.getRGB(0, 0, size, image.getHeight(), null, 0, size);
            var anim = assets.containsKey(path + ".mcmeta")
                ? json(assets, path + ".mcmeta").getAsJsonObject("animation")
                : new JsonObject();
            frameTime = anim.has("frametime") ? anim.get("frametime")
                .getAsInt() : 1;
            interpolate = anim.has("interpolate") && anim.get("interpolate")
                .getAsBoolean();
            int count = image.getHeight() / size;
            int[] sequence;
            if (count > 1 && anim.has("frames")) {
                var entries = anim.getAsJsonArray("frames");
                sequence = IntStream.range(0, entries.size())
                    .map(
                        i -> entries.get(i)
                            .getAsInt())
                    .filter(i -> i >= 0 && i < count)
                    .toArray();
            } else sequence = IntStream.range(0, count)
                .toArray();
            frames = sequence.length == 0 ? new int[] { 0 } : sequence;
        }

        void bind(int age) {
            if (gpu == null) gpu = new DynamicTexture(size, size);
            int step = Math.floorMod(age, frames.length * frameTime);
            if (step != last) {
                int frame = step / frameTime, next = (frame + 1) % frames.length,
                    weight = interpolate ? step % frameTime : 0;
                int[] dst = gpu.getTextureData();
                int offset = frames[frame] * dst.length;
                if (weight == 0) System.arraycopy(pixels, offset, dst, 0, dst.length);
                else for (int i = 0; i < dst.length; i++) {
                    int a = pixels[offset + i], b = pixels[frames[next] * dst.length + i];
                    int color = a & 0xff000000;
                    for (int shift = 0; shift <= 16; shift += 8)
                        color |= ((((a >>> shift) & 255) * (frameTime - weight) + ((b >>> shift) & 255) * weight)
                            / frameTime) << shift;
                    dst[i] = color;
                }
                gpu.updateDynamicTexture();
                last = step;
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, gpu.getGlTextureId());
        }
    }
}
