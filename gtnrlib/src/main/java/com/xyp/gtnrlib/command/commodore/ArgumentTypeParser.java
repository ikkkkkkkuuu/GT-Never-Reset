package com.xyp.gtnrlib.command.commodore;

import com.xyp.gtnrlib.command.brigadier.arguments.ArgumentType;

public interface ArgumentTypeParser {

    boolean canParse(String var1, String var2);

    ArgumentType<?> parse(String var1, String var2, TokenStream var3) throws ParseException;
}
