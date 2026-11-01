package com.xyp.gtnrlib.mixins.early.command;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnrlib.client.command.GuiTextFieldExtras;
import com.xyp.gtnrlib.client.gui.command.CommandSuggestions;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Connects Brigadier suggestions to the vanilla chat screen. */
@Mixin(GuiChat.class)
@SideOnly(Side.CLIENT)
public class GuiChatMixin extends GuiScreen {

    @Shadow
    protected GuiTextField inputField;
    @Shadow
    private String defaultInputFieldText;
    @Unique
    private CommandSuggestions gtnrlib$commandSuggestions;
    @Unique
    private String gtnrlib$lastText = "";

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        this.gtnrlib$initializeCommandSuggestions();
    }

    @Unique
    private void gtnrlib$initializeCommandSuggestions() {
        ((GuiTextFieldExtras) this.inputField).gtnrlib$chatInput(true);
        CommandSuggestions.CommandSuggestionsConfig config = new CommandSuggestions.CommandSuggestionsConfig(
            false,
            false,
            1,
            10,
            true,
            -805306368);
        this.gtnrlib$commandSuggestions = new CommandSuggestions(
            this.mc,
            this,
            this.inputField,
            this.fontRendererObj,
            config,
            (lines, x, y) -> drawHoveringText(lines, x, y, this.fontRendererObj));
        this.gtnrlib$commandSuggestions.updateCommandInfo();
        this.gtnrlib$commandSuggestions.refreshCommands();
        this.gtnrlib$lastText = this.inputField.getText();
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
    private void onHandleMouseInput(CallbackInfo ci) {
        if (this.gtnrlib$commandSuggestions != null
            && this.gtnrlib$commandSuggestions.handleMouseScroll(Mouse.getEventDWheel())) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (this.gtnrlib$commandSuggestions != null
            && this.gtnrlib$commandSuggestions.handleMouseClick(mouseX, mouseY, mouseButton)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (this.gtnrlib$commandSuggestions != null) {
            // Poll for text changes since 1.7.10 has no GuiResponder
            String currentText = this.inputField.getText();
            if (!currentText.equals(this.gtnrlib$lastText)) {
                this.gtnrlib$lastText = currentText;
                this.gtnrlib$commandSuggestions.allowSuggestions(!currentText.equals(this.defaultInputFieldText));
                this.gtnrlib$commandSuggestions.updateCommandInfo();
            }
            this.gtnrlib$commandSuggestions.render(mouseX, mouseY);
        }
    }

    @Override
    public void handleKeyboardInput() {
        if (Keyboard.getEventKeyState() && this.gtnrlib$commandSuggestions != null
            && this.gtnrlib$commandSuggestions.handleKeyPress(Keyboard.getEventKey())) return;
        super.handleKeyboardInput();
    }
}
