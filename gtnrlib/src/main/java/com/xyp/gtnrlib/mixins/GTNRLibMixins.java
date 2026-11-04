package com.xyp.gtnrlib.mixins;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.xyp.gtnrlib.mod.ModList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GTNRLibMixins implements IMixins {

    FIRST_PERSON_ANIMATION(new MixinBuilder("Optional first-person pose application")
        .addClientMixins(
            "animation.ItemRendererMixin",
            "animation.LocalSwingMixin",
            "animation.MinecraftAttackMixin",
            "animation.ItemBlockPlacementMixin")
        .setPhase(Phase.EARLY)),

    KEY_INPUT(new MixinBuilder("Key binding input access").addClientMixins("input.KeyBindingAccessor")
        .setPhase(Phase.EARLY)),

    COMMAND_TREE(new MixinBuilder("Chat command suggestions")
        .addClientMixins("command.GuiChatMixin", "command.GuiTextFieldMixin", "command.NetHandlerPlayClientMixin")
        .setPhase(Phase.EARLY)),

    TEXT_EFFECTS_COMMON(new MixinBuilder("Text effect formatting codes")
        .addClientMixins("texteffect.MixinEnumChatFormattingTextEffects")
        .setPhase(Phase.EARLY)),

    TEXT_EFFECTS_CLIENT(new MixinBuilder("Text effect rendering")
        .addClientMixins(
            "texteffect.MixinFontRendererTextEffects",
            "texteffect.MixinGuiNewChatTextEffects",
            "texteffect.MixinGuiTextFieldTextEffects")
        .setPhase(Phase.EARLY)),

    TEXT_EFFECTS_ANGELICA(new MixinBuilder("Optional Angelica text effect batching")
        .addClientMixins("texteffect.angelica.MixinFontBatchTextEffects")
        .setPhase(Phase.EARLY)
        .addRequiredMod(ModList.Angelica)),

    TEXT_EFFECTS_NEI(
        new MixinBuilder("Optional NEI text effect input").addClientMixins("texteffect.MixinNEIFormattedTextField")
            .setPhase(Phase.LATE)
            .addRequiredMod(ModList.NotEnoughItems)),

    THAUMCRAFT_WAND(new MixinBuilder("First-person wand handle").addClientMixins("thaumcraft.WandHandleMixin")
        .setPhase(Phase.LATE)
        .addRequiredMod(ModList.Thaumcraft));

    private final MixinBuilder builder;
}
