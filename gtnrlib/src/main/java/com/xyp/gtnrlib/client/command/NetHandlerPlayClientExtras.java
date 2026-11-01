package com.xyp.gtnrlib.client.command;

import org.jetbrains.annotations.NotNull;

import com.xyp.gtnrlib.command.brigadier.CommandDispatcher;
import com.xyp.gtnrlib.command.tree.ISuggestionProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface NetHandlerPlayClientExtras {

    ClientSuggestionProvider gtnrlib$suggestionsProvider();

    @NotNull
    CommandDispatcher<ISuggestionProvider> gtnrlib$commands();

    void gtnrlib$setCommands(@NotNull CommandDispatcher<ISuggestionProvider> dispatcher);
}
