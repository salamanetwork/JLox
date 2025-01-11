package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

/*
    The scanner works its way through the source code, adding tokens until it runs out of characters.
    Then it appends one final “end of file” token.
    That isn’t strictly needed, but it makes our parser a little cleaner.
*/

public class Scanner {

    // Scanner Data
    // We store the raw source code as a simple string.
    private final String source;    // Holding The  Source String

    // We have a list ready to fill with tokens we’re going to generate
    private final List<Token> tokens = new ArrayList<>();   // Holding the tokens' list

    // To keep track of where the scanner is in the source code:

    // The start and current fields are offsets that index into the string.
    private int start = 0;      // Points to the first character in the lexeme being scanned
    private int current = 0;    // Points at the character currently being considered

    // The line field tracks what source line (current) is on so we can produce tokens that know their location.
    private int line = 1;       // Points to How many lines as a counter increased by '\n' when it is found.

    // To get identifiers working:
    // To handle keywords, we see if the identifier’s lexeme is one of the reserved words.
    // If so, we use a token type specific to that keyword.
    // We define the set of reserved words in a map.
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fn",    FUN);
        keywords.put("fun",    FUN);
        keywords.put("func",    FUN);
        keywords.put("function",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }


    Scanner(String source) {
        this.source = source;
    }

    // The aforementioned loop that going to generate that tokens
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        // Then it appends one final “end of file” token.
        tokens.add(new Token(EOF, "", null, line));

        // Return the list of the generated tokens.
        return tokens;
    }

    // In each turn of the loop, we scan a single token. This is the real heart of the scanner.
    // Imagine if every lexeme were only a single character long.
    // All you would need to do is consume the next character and pick a token type for it.
    private void scanToken() {
        char c = advance();
        switch (c) {

            // Several lexemes are only a single character in Lox, so let’s start with those:
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;

            // Operators !=, ==, >=, <= Scanned as a single lexeme (if Not scan as a single character only):
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);                           // != or !
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);                         // == or =
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);                           // <= or <
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);                     // >= or >
                break;

            // Handling Longer Lexemes:
            // Handling operator '/' for division.
            // That character needs a little special handling because comments begin with a slash too.
            // Comments are lexemes, but they aren’t meaningful, and the parser doesn’t want to deal with them.
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;

            // Handling Whitespaces:
            // When encountering whitespace, we simply go back to the beginning of the scan loop.
            // That starts a new lexeme after the whitespace character.

            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;

            // Handling the New Line:
            // For newlines, we do the same thing, but we also increment the line counter.
            // This is why we used peek() to find the newline ending a comment instead of match().
            // We want that newline to get us here so we can update line.
            case '\n':
                line++;
                break;

            // Handling String literals:
            case '"':
                string();
                break;

            // Handling (Number, String, Identifier, Keyword, Error)
            default:

                // Handling Number literals:
                // All numbers in Lox are floating point at runtime, but both integer and decimal literals are supported.
                // A number literal is a series of digits optionally followed by a '.' and one or more trailing digits:
                // Examples: 1234 is an Integer, 12.34 is a Float
                // We don’t allow a leading or trailing decimal point, so these are both invalid:
                // Examples: .1234 is Invalid, 1234. is Invalid
                // To recognize the beginning of a number lexeme, we look for any digit.
                // It’s kind of tedious to add cases for every decimal digit, so we’ll stuff it in the default case instead.
                if(isDigit(c)) {

                    number();


                }

                // Handling Reserved Words and Identifiers:
                /*
                 * Special Case:
                 *   Consider what would happen if a user named a variable orchid.
                 *   The scanner would see the first two letters, 'or', and immediately emit an or keyword token.
                 * -----------------------------------------------------------------------------------------------------------------------------------------------
                 * Maximal Munch:
                 *   - Means we can’t easily detect a reserved word until we’ve reached the end of what might instead be an identifier.
                 *   - When two lexical grammar rules can both match a chunk of code that the scanner is looking at, whichever one matches the most characters wins.
                 *   - That rule states that if we can match orchid as an identifier and or as a keyword, then the former wins.
                 *   - This is also why we tacitly assumed, previously, that <= should be scanned as a single <= token and not < followed by =.
                 *   - After all, a reserved word is an identifier, it’s just one that has been claimed by the language for its own use.
                 *   - That’s where the term reserved word comes from.
                 * -----------------------------------------------------------------------------------------------------------------------------------------------
                 */
                else if(isAlpha(c)) {

                    identifier();

                }

                // Handling Errors
                else {

                    // let’s take a moment to think about errors at the lexical level.
                    Lox.error(line, "Unexpected character '" + c + "'");
                }
            break;
        }
    }

    // Handling Reserved Words and Identifiers:
    private void identifier() {

        // That gets identifiers working.
        while (isAlphaNumeric(peek())) advance();

        // Then, after we scan an identifier, we check to see if it matches anything in the map.
        // If so, we use that keyword’s token type. Otherwise, it’s a regular user-defined identifier.
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if(type == null) {
            type = IDENTIFIER;
        }

        addToken(type);
    }

    // Handling Number literals lexeme:
    private void number() {

        // Our interpreter uses Java’s Double type to represent numbers, so we produce a value of that type.
        // We’re using Java’s own parsing method to convert the lexeme to a real Java double.
        // We consume as many digits as we find for the integer part of the literal.
        // Then we look for a fractional part, which is a decimal point (.) followed by at least one digit.
        // If we do have a fractional part, again, we consume as many digits as we can find.
        // Looking past the decimal point requires a second character of lookahead since we don’t want to consume the .
        // until we’re sure there is a digit after it. So we add peekNext() method

        // Handling Number literals lexeme:
        // Loop to find the sequence of digits
        while (isDigit(peek())) {
            advance();
        }

        // Look for a fractional part:
        // Logic: if the current is Digit peek the next character with peek() method if is Digit continue,
        // And peek at the same time the next character with peekNext() method if is a digit continue to consume.
        if(peek() == '.' && isDigit(peekNext())) {

            // Consume the "."
            advance();

            // Keep the loop if it is still a digit
            while (isDigit(peek())) {
                advance();
            }
        }

        // We convert the lexeme to its numeric value.
        // Add the Token:
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));

    }

    // Handling String and its Value:
    // Like with comments, we consume characters until we hit the " that ends the string.
    // We also gracefully handle running out of input before the string is closed and report an error for that.
    private void string() {

        // Handling string literal lexeme:
        // Loop with Check of next character != '"' and also is not at the end of the file != EOF:
        while(peek() != '"' && !isAtEnd()) {

            // For no particular reason, Lox supports multi-line strings.
            // That does mean we also need to update line when we hit a newline inside a string.
            if(peek() == '\n') {
                line++;
            }

            advance();
        }

        if(isAtEnd()) {
            Lox.error(line, "Unexpected end of string.");
            return;
        }

        // Keep in advance to closing ''.
        advance();

        // We also produce the actual string value that will be used later by the interpreter.
        // That conversion only requires a substring() to strip off the surrounding quotes.
        // TODO: If Lox supported escape sequences like '\n', '\r', '\\', ..., we’d unescape those here.
        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);

        //  The last interesting bit is that when we create the token.
        addToken(STRING, value);

    }

    // Helper methods [advance, match, peek, peekNext, isAlpha, isAlphaNumeric, isDigit, addToken, isAtEnd]

    // Technically, match() is doing lookahead too.
    // advance() and peek() are the fundamental operators and match() combines them.

    // The advance() method consumes the next character in the source file and returns it.
    private char advance() {
        return source.charAt(current++);
    }

    // It’s like a conditional advance().
    // We only consume the current character if it’s what we’re looking for.
    // Using match(), we recognize these lexemes in two stages.
    // When we reach, for example, !, we jump to its switch case.
    // That means we know the lexeme starts with !.
    // Then we look at the next character to determine if we’re on a != or merely a !.
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    // The peek() method: It’s sort of like advance(), but doesn’t consume the character.
    // This is called lookahead.
    // Since it only looks at the current unconsumed character, we have one character of lookahead.
    private char peek() {
        if(isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // The peekNext() method: It’s sort of like peek(), but check the character after you peek a lookahead peek().
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    // The isAlpha() method: to look for any alphabets.
    // So we begin by assuming any lexeme starting with a letter or underscore is an identifier.
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    // The isAlphaNumeric() method: to check if the character 'c' is Alphabetic Or Numeric
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // The isDigit() method: to look for any digit.
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // Where advance() is for input, addToken() is for output.
    // It grabs the text of the current lexeme and creates a new token for it.
    // We’ll use the other overload to handle tokens with literal values soon.
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    // Helper function that tells us if we’ve consumed all the characters.
    private boolean isAtEnd() {
        return current >= source.length();
    }

}
