package com.xyp.gtnrlib.command.brigadier.exceptions;

import com.xyp.gtnrlib.command.brigadier.ImmutableStringReader;
import com.xyp.gtnrlib.command.brigadier.Message;

public class SimpleCommandExceptionType implements CommandExceptionType {

    private final Message message;

    public SimpleCommandExceptionType(Message message) {
        this.message = message;
    }

    public CommandSyntaxException create() {
        return new CommandSyntaxException(this, this.message);
    }

    public CommandSyntaxException createWithContext(ImmutableStringReader reader) {
        return new CommandSyntaxException(this, this.message, reader.getString(), reader.getCursor());
    }

    @Override
    public String toString() {
        return this.message.getString();
    }
}
