package com.xyp.gtnrlib.client.text.effect;

import net.minecraft.util.ResourceLocation;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.client.shader.GlslProgram;

public class TextShader extends GlslProgram {

    public TextShader(ResourceLocation fragment) {
        super(new ResourceLocation(GTNRLib.MODID, "shaders/text/text.vert.glsl"), fragment);
    }
}
