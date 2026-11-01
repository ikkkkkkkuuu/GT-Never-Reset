package com.xyp.gtnrlib.command.brigadier.exceptions;

import java.util.function.Function;

import com.xyp.gtnrlib.command.brigadier.ImmutableStringReader;
import com.xyp.gtnrlib.command.brigadier.Message;

public class DynamicCommandExceptionType implements CommandExceptionType {

    private final Function<Object, Message> function;

    public DynamicCommandExceptionType(Function<Object, Message> function) {
        this.function = function;
    }

    public CommandSyntaxException create(Object arg) {
        return new CommandSyntaxException(this, this.function.apply(arg));
    }

    public CommandSyntaxException createWithContext(ImmutableStringReader reader, Object arg) {
        return new CommandSyntaxException(this, this.function.apply(arg), reader.getString(), reader.getCursor());
    }
}
