package com.xyp.gtnrlib.command.brigadier.builder;

import com.xyp.gtnrlib.command.brigadier.tree.CommandNode;
import com.xyp.gtnrlib.command.brigadier.tree.LiteralCommandNode;

import lombok.Getter;

public class LiteralArgumentBuilder<S> extends ArgumentBuilder<S, LiteralArgumentBuilder<S>> {

    @Getter
    private final String literal;

    protected LiteralArgumentBuilder(String literal) {
        this.literal = literal;
    }

    public static <S> LiteralArgumentBuilder<S> literal(String name) {
        return new LiteralArgumentBuilder<>(name);
    }

    protected LiteralArgumentBuilder<S> getThis() {
        return this;
    }

    public LiteralCommandNode<S> build() {
        LiteralCommandNode<S> result = new LiteralCommandNode<>(
            this.getLiteral(),
            this.getCommand(),
            this.getRequirement(),
            this.getRedirect(),
            this.getRedirectModifier(),
            this.isFork());

        for (CommandNode<S> argument : this.getArguments()) {
            result.addChild(argument);
        }

        return result;
    }
}
