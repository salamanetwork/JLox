package com.craftinginterpreters.lox;

public class Token {

    // Token Data
    final TokenType type;       // Token Type
    final String lexeme;        // Store the Lexeme
    final Object literal;       // Store the actual value that represents the lexeme (number, string, boolean, ...)
    final int line;             // Store the location Information

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    // To print out the current token
    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
