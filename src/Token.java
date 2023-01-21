/**

 The Token class represents a token in Lox programming language.

 @param  type     represents the type of token, such as keyword, identifier, etc.

 @param  lexeme   represents the actual string of characters that make up the token.

 @param  literal  represents the value of the token, such as a numeric value or a string.

 @param  line     represents the line number on which the token appears in the source code.
 */
class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
