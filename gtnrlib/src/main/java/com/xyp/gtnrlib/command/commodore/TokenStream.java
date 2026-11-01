package com.xyp.gtnrlib.command.commodore;

public interface TokenStream {

    boolean hasNext();

    Token next();

    Token peek();

    ParseException createException(String var1);

    ParseException createException(Throwable var1);

    ParseException createException(String var1, Throwable var2);
}
