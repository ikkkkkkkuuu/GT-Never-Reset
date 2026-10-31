package com.xyp.gtnrlib.mixins.early.input;

import net.minecraft.client.settings.KeyBinding;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {

    @Accessor("pressed")
    void gtnrlib$setPressed(boolean pressed);

    @Accessor("pressed")
    boolean gtnrlib$isPressed();

    @Accessor("pressTime")
    int gtnrlib$getPressTime();

    @Accessor("pressTime")
    void gtnrlib$setPressTime(int pressTime);
}
