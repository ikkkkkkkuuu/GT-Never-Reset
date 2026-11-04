package com.xyp.gtnrlib.compat.tconstruct;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.mixins.late.tconstruct.CrossbowAmmoAccess;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import tconstruct.client.FlexibleToolRenderer;
import tconstruct.library.tools.ToolCore;
import tconstruct.library.weaponry.IWindup;
import tconstruct.weaponry.TinkerWeaponry;
import tconstruct.weaponry.client.item.CrossbowRenderer;

@SideOnly(Side.CLIENT)
public final class TinkersCrossbowAnimation implements HmiItemBindings.TwoHandedModel {

    // Rear-shaft grip in stock coordinates, from below the left edge of the view to the flight groove.
    private static final Vector3fc PICKUP = new Vector3f(-.85f, -.94f, .10f);
    private static final Vector3fc PRESENT = new Vector3f(-.86f, .38f, -.13f);
    private static final Vector3fc ABOVE_RAIL = new Vector3f(0, .22f, -.045f);
    private static final Vector3fc SEATED = new Vector3f(0, .063f, -.16f);
    private static final Vector3fc FOREGRIP = new Vector3f(-.14f, -.09f, -.43f);
    // The resting 16px string crosses the stock 0.32 units ahead of the rear grip, between the two limb tips.
    private static final float LIMB_SPAN = .50f, REST_Z = -.318f, CATCH_Z = .012f, CATCH_HEIGHT = .038f;
    private static final float BOLT_IN_HAND = .65f;
    private static final DoubleBuffer LEFT_HALF = halfPlane(-1), RIGHT_HALF = halfPlane(1);
    // FlexibleToolRenderer reads ten part and modifier passes; the crossbow string occupies pass two.
    private static final int TOOL_PASSES = 10, STRING_PASS = 2;
    private final FlexibleToolRenderer parts = new FlexibleToolRenderer();
    private final Matrix4f stock = new Matrix4f();
    private final Matrix4f stringShear = new Matrix4f();
    private final FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
    private final Vector3f grip = new Vector3f();
    private final Vector3f boltGrip = new Vector3f(), support = new Vector3f();
    private ItemStack sampledBow, sampledAmmo;
    private int ammoTick;
    private boolean sampledLoaded;

    private TinkersCrossbowAnimation() {}

    public static void register() {
        if (TinkerWeaponry.crossbow != null)
            HmiItemBindings.registerTool(TinkerWeaponry.crossbow, new TinkersCrossbowAnimation());
    }

    public static void fire(ItemStack stack) {
        var renderer = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON);
        if (renderer != null && renderer.getClass() == CrossbowRenderer.class)
            FirstPersonItemActions.INSTANCE.trigger("gtnrlib:crossbow_fire");
    }

    @Override
    public boolean matches(ItemStack stack, IItemRenderer renderer) {
        return renderer.getClass() == CrossbowRenderer.class && stack.hasTagCompound();
    }

    @Override
    public void renderGripped(ItemStack stack, IItemRenderer renderer, float partial, HmiItemBindings.GripHands hands) {
        var stats = stack.getTagCompound()
            .getCompoundTag("InfiTool");
        int remaining = stats.getInteger("Reloading");
        float progress = remaining > 0
            ? 1 - (remaining - partial) / Math.max(1, ((IWindup) stack.getItem()).getWindupTime(stack))
            : 0;
        boolean loaded = stats.getBoolean("Loaded");
        float reach = smooth(progress / .13f);
        float tension = loaded ? 1 : smooth((progress - .13f) / .37f);
        float fetch = smooth((progress - .55f) / (BOLT_IN_HAND - .55f));
        float lift = smooth((progress - BOLT_IN_HAND) / .09f);
        float align = smooth((progress - .76f) / .09f);
        float seat = smooth((progress - .85f) / .07f);
        float release = smooth((progress - .92f) / .08f);
        float tilt = reach * (1 - release);
        if (loaded) boltGrip.set(SEATED);
        else boltGrip.set(PICKUP)
            .lerp(PRESENT, lift)
            .lerp(ABOVE_RAIL, align)
            .lerp(SEATED, seat);

        // Pivot at the rear grip to expose the rail to the support hand while winding.
        GL11.glTranslatef(.22f, .10f, -.99f);
        stock.rotationYXZ(
            (float) Math.toRadians(16 + 13 * tilt),
            (float) Math.toRadians(8 + 15 * tilt),
            (float) Math.toRadians(28 * tilt));
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationItemsTexture);
        drawBow(stack, (CrossbowRenderer) renderer, tension);
        if (loaded || progress >= BOLT_IN_HAND) {
            var ammo = ammunition(stack, loaded);
            if (ammo != null) drawBolt(ammo, loaded ? 0 : 1 - align);
        } else {
            sampledBow = null;
            sampledAmmo = null;
        }
        hand(hands, false, .035f, -.06f, .015f, 25);
        // The hooked fist tracks the same nocking point that bends the string. Leave it on the catch during pickup.
        grip.set(-.073f, CATCH_HEIGHT * tension - .055f, REST_Z + stringTravel(tension));
        support.set(FOREGRIP)
            .lerp(grip, reach);
        grip.set(boltGrip)
            .add(-.055f, -.077f, .015f);
        support.lerp(grip, fetch)
            .lerp(FOREGRIP, release);
        hand(hands, true, support.x, support.y, support.z, -38);
    }

    private ItemStack ammunition(ItemStack bow, boolean loaded) {
        var player = Minecraft.getMinecraft().thePlayer;
        if (bow != sampledBow || player.ticksExisted != ammoTick || loaded != sampledLoaded) {
            var source = (CrossbowAmmoAccess) bow.getItem();
            sampledAmmo = loaded ? source.gtnrlib$loadedBolt(bow) : source.gtnrlib$findBolt(player, bow);
            sampledBow = bow;
            ammoTick = player.ticksExisted;
            sampledLoaded = loaded;
        }
        return sampledAmmo;
    }

    private void drawBow(ItemStack stack, CrossbowRenderer renderer, float tension) {
        var strings = ((ToolCore) stack.getItem()).accessoryIcons;
        int material = stack.getTagCompound()
            .getCompoundTag("InfiTool")
            .getInteger("RenderAccessory");
        IIcon restingString = strings.getOrDefault(material, strings.get(-1));
        GL11.glPushAttrib(GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT | GL11.GL_TRANSFORM_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glMultMatrix(stock.get(matrix));
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, .1f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            for (int pass = 0; pass < TOOL_PASSES; pass++) {
                var icon = stack.getItem()
                    .getIcon(stack, pass);
                if (icon == null || icon == ToolCore.blankSprite || icon == ToolCore.emptyIcon) continue;
                int rgb = stack.getItem()
                    .getColorFromItemStack(stack, pass);
                GL11.glColor4ub((byte) (rgb >> 16), (byte) (rgb >> 8), (byte) rgb, (byte) 255);
                if (pass == STRING_PASS && restingString != null && restingString != ToolCore.emptyIcon) {
                    GL11.glEnable(GL11.GL_CLIP_PLANE0);
                    stringHalf(-1, LEFT_HALF, renderer.depth, tension, restingString);
                    stringHalf(1, RIGHT_HALF, renderer.depth, tension, restingString);
                    GL11.glDisable(GL11.GL_CLIP_PLANE0);
                } else drawLayer(icon, renderer.depth);
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void stringHalf(int side, DoubleBuffer plane, float depth, float tension, IIcon icon) {
        // Shear each half of the material sprite around its fixed limb tip; both meet exactly at the moving nock.
        float rise = CATCH_HEIGHT * tension, travel = stringTravel(tension);
        stringShear.translation(0, rise, travel)
            .m01(-side * rise / LIMB_SPAN)
            .m02(-side * travel / LIMB_SPAN);
        GL11.glClipPlane(GL11.GL_CLIP_PLANE0, plane);
        GL11.glPushMatrix();
        try {
            GL11.glMultMatrix(stringShear.get(matrix));
            drawLayer(icon, depth);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static float stringTravel(float tension) {
        return (CATCH_Z - REST_Z) * tension;
    }

    private static DoubleBuffer halfPlane(int side) {
        var plane = BufferUtils.createDoubleBuffer(4);
        plane.put(0, side);
        return plane;
    }

    private static void drawLayer(IIcon icon, float depth) {
        GL11.glPushMatrix();
        try {
            // The stock runs along the lower-right to upper-left diagonal of the 16px sprite.
            GL11.glRotatef(-90, 1, 0, 0);
            GL11.glRotatef(45, 0, 0, 1);
            GL11.glScalef(.94f, .94f, .94f);
            GL11.glTranslatef(-.23f, -.23f, depth);
            ItemRenderer.renderItemIn2D(
                Tessellator.instance,
                icon.getMaxU(),
                icon.getMinV(),
                icon.getMinU(),
                icon.getMaxV(),
                icon.getIconWidth(),
                icon.getIconHeight(),
                2 * depth);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private void drawBolt(ItemStack ammo, float across) {
        GL11.glPushMatrix();
        try {
            GL11.glMultMatrix(stock.get(matrix));
            GL11.glTranslatef(boltGrip.x, boltGrip.y, boltGrip.z);
            GL11.glRotatef(-76 * across, 0, 1, 0);
            GL11.glRotatef(12 * across, 1, 0, 0);
            // Two crossed material sprites give the head and fletching a silhouette seen along the rail.
            parts.setDepth(1f / 64);
            boltPlane(ammo);
            GL11.glRotatef(90, 0, 0, 1);
            boltPlane(ammo);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private void boltPlane(ItemStack ammo) {
        GL11.glPushMatrix();
        try {
            GL11.glRotatef(-90, 1, 0, 0);
            GL11.glRotatef(-45, 0, 0, 1);
            GL11.glScalef(.75f, .75f, .75f);
            GL11.glTranslatef(-.66f, -.34f, 0);
            parts.renderItem(ItemRenderType.EQUIPPED_FIRST_PERSON, ammo);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private void hand(HmiItemBindings.GripHands hands, boolean left, float x, float y, float z, float roll) {
        GL11.glPushMatrix();
        try {
            // The fists follow the rotating stock, while the upper arms extend back towards their own shoulders.
            stock.transformPosition(x, y, z, grip);
            GL11.glTranslatef(grip.x, grip.y, grip.z);
            GL11.glRotatef(roll, 0, 0, 1);
            GL11.glRotatef(-50, 1, 0, 0);
            if (left) GL11.glScalef(1, 1.14f, 1);
            hands.draw(left);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static float smooth(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
