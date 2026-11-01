package com.xyp.gtnrlib.command.commodore;

import lombok.Getter;

public interface Token {

    public static enum ConstantToken implements Token {
        OPEN_BRACKET,
        CLOSE_BRACKET,
        SEMICOLON,
        EOF;
    }

    public static final class StringToken implements Token {

        @Getter
        private final String string;

        StringToken(String string) {
            this.string = string;
        }

    }
}
