package com.xyp.gtneverreset.client.action.radialmenu;

import com.xyp.gtneverreset.utils.gui.radialmenu.ActionSpec;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionTypes;
import com.xyp.gtneverreset.utils.gui.radialmenu.Placeholders;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CommandActionExecutor implements IActionExecutor {

    @Override
    public String typeId() {
        return ActionTypes.COMMAND;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        String[] lines = Placeholders.splitLines(spec.getString(ActionTypes.PARAM_COMMAND, ""));
        if (lines.length == 0) {
            return false;
        }

        if (spec.getBoolean(ActionTypes.PARAM_CYCLE, false)) {
            int index = Math.floorMod(spec.cycleCursor, lines.length);
            spec.cycleCursor = index + 1;
            ChatSender.send(lines[index]);
            return true;
        }

        int delay = Math.max(0, spec.getInt(ActionTypes.PARAM_DELAY_TICKS, 0));

        if (delay == 0) {
            for (String line : lines) {
                ChatSender.send(line);
            }
            return true;
        }

        DelayedActions.cancel(spec);
        ChatSender.send(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            DelayedActions.scheduleCommand(spec, lines[i], delay * i);
        }
        return true;
    }
}
