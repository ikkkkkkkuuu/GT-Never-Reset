package com.xyp.gtnrlib.mixins.early.command;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S3APacketTabComplete;
import net.minecraftforge.client.ClientCommandHandler;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnrlib.client.command.ClientCommandTree;
import com.xyp.gtnrlib.client.command.ClientSuggestionProvider;
import com.xyp.gtnrlib.client.command.NetHandlerPlayClientExtras;
import com.xyp.gtnrlib.command.brigadier.CommandDispatcher;
import com.xyp.gtnrlib.command.tree.ISuggestionProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mixin(NetHandlerPlayClient.class)
@SideOnly(Side.CLIENT)
public class NetHandlerPlayClientMixin implements NetHandlerPlayClientExtras {

    @Shadow
    private Minecraft gameController;

    @Unique
    private ClientSuggestionProvider gtnrlib$suggestionsProvider;
    @Unique
    private CommandDispatcher<ISuggestionProvider> gtnrlib$commands = new CommandDispatcher<ISuggestionProvider>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.gtnrlib$suggestionsProvider = new ClientSuggestionProvider(
            (NetHandlerPlayClient) (Object) this,
            this.gameController);
    }

    @Inject(method = "handleTabComplete", at = @At("TAIL"))
    private void handleTabComplete(S3APacketTabComplete packetIn, CallbackInfo ci) {
        this.gtnrlib$suggestionsProvider.completeCustomSuggestions(packetIn.func_149630_c());
    }

    @Unique
    @Override
    public ClientSuggestionProvider gtnrlib$suggestionsProvider() {
        return this.gtnrlib$suggestionsProvider;
    }

    @Unique
    @NotNull
    @Override
    public CommandDispatcher<ISuggestionProvider> gtnrlib$commands() {
        if (this.gameController.thePlayer != null) {
            ClientCommandTree.merge(
                this.gtnrlib$commands,
                ClientCommandHandler.instance.getCommands(),
                this.gameController.thePlayer);
        }
        return this.gtnrlib$commands;
    }

    @Unique
    @Override
    public void gtnrlib$setCommands(@NotNull CommandDispatcher<ISuggestionProvider> dispatcher) {
        this.gtnrlib$commands = dispatcher;
        gtnrlib$commands();
    }
}
