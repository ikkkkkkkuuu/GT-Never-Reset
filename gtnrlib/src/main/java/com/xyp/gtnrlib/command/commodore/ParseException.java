package com.xyp.gtnrlib.command.commodore;

public final class ParseException extends Exception {

    ParseException(String message, int currentLine) {
        super(message + " (at line " + currentLine + ")");
    }

    ParseException(Throwable cause, int currentLine) {
        super("At line " + currentLine, cause);
    }

    ParseException(String message, Throwable cause, int currentLine) {
        super(message + " (at line " + currentLine + ")", cause);
    }
}
