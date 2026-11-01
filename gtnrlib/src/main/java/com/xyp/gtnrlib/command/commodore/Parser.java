package com.xyp.gtnrlib.command.commodore;

import java.util.Arrays;
import java.util.Collection;

import com.xyp.gtnrlib.command.brigadier.arguments.ArgumentType;
import com.xyp.gtnrlib.command.brigadier.builder.ArgumentBuilder;
import com.xyp.gtnrlib.command.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnrlib.command.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnrlib.command.brigadier.tree.CommandNode;
import com.xyp.gtnrlib.command.brigadier.tree.LiteralCommandNode;

class Parser<S> {

    private final Lexer lexer;
    private final Collection<ArgumentTypeParser> argumentTypeParsers;

    Parser(Lexer lexer, Collection<ArgumentTypeParser> argumentTypeParsers) {
        this.lexer = lexer;
        this.argumentTypeParsers = argumentTypeParsers;
    }

    LiteralCommandNode<S> parse() throws ParseException {
        CommandNode<S> node = this.parseNode();
        if (!(node instanceof LiteralCommandNode)) {
            throw this.lexer.createException("Root command node is not a literal command node");
        } else if (this.lexer.peek() != Token.ConstantToken.EOF) {
            throw this.lexer.createException("Expected end of file but got " + this.lexer.peek());
        } else {
            return (LiteralCommandNode<S>) node;
        }
    }

    private CommandNode<S> parseNode() throws ParseException {
        Token token = this.lexer.next();
        if (!(token instanceof Token.StringToken)) {
            throw this.lexer.createException("Expected string token for node name but got " + token);
        } else {
            String name = ((Token.StringToken) token).getString();
            ArgumentBuilder<S, ?> node;
            if (this.lexer.peek() instanceof Token.StringToken) {
                node = RequiredArgumentBuilder.argument(name, this.parseArgumentType());
            } else {
                node = LiteralArgumentBuilder.literal(name);
            }

            if (this.lexer.peek() == Token.ConstantToken.OPEN_BRACKET) {
                this.lexer.next();

                while (this.lexer.peek() != Token.ConstantToken.CLOSE_BRACKET) {
                    CommandNode<S> child = this.parseNode();
                    node.then(child);
                }

                this.lexer.next();
            } else {
                if (this.lexer.peek() != Token.ConstantToken.SEMICOLON) {
                    throw this.lexer
                        .createException("Node definition not ended with semicolon, got " + this.lexer.peek());
                }

                this.lexer.next();
            }

            return node.build();
        }
    }

    private ArgumentType<?> parseArgumentType() throws ParseException {
        Token token = this.lexer.next();
        if (!(token instanceof Token.StringToken)) {
            throw this.lexer.createException("Expected string token for argument type but got " + token);
        } else {
            String argumentType = ((Token.StringToken) token).getString();
            String[] key = argumentType.split(":");
            if (key.length != 2) {
                throw this.lexer.createException("Invalid key for argument type: " + Arrays.toString((Object[]) key));
            } else {
                for (ArgumentTypeParser parser : this.argumentTypeParsers) {
                    if (parser.canParse(key[0], key[1])) {
                        return parser.parse(key[0], key[1], this.lexer);
                    }
                }

                throw this.lexer.createException("Unable to parse argument type: " + argumentType);
            }
        }
    }
}
