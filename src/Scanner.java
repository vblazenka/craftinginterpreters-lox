import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    // points to the first character in the lexeme being scanned
    private int start = 0;
    // points to the current character in the lexeme being considered
    private int current = 0;
    // tracks on what source line `current` is on so that we can produce tokens that know their location
    private int line = 1;
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();

        keywords.put("and", TokenType.AND);
        keywords.put("class", TokenType.CLASS);
        keywords.put("else",  TokenType.ELSE);
        keywords.put("false",  TokenType.FALSE);
        keywords.put("for",    TokenType.FOR);
        keywords.put("fun",    TokenType.FUN);
        keywords.put("if",     TokenType.IF);
        keywords.put("nil",    TokenType.NIL);
        keywords.put("or",     TokenType.OR);
        keywords.put("print",  TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super",  TokenType.SUPER);
        keywords.put("this",   TokenType.THIS);
        keywords.put("true",   TokenType.TRUE);
        keywords.put("var",    TokenType.VAR);
        keywords.put("while",  TokenType.WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    /**
     * Works its way through the source code, adding tokens until it runs
     * out of characters. Then it appends one final "end of file" token.
     *
     * @return list of tokens
     */
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;
            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;
            case '/':
                if (match('/')) {
                    // a comment goes until the end of the line, so we consume characters until the end of the line
                    // comment is not a useful lexeme, so we don't add it to the token
                    while (peek() != '\n' && !isAtEnd()) {
                        if (peek() == '\n') {
                            line++;
                        }

                        advance();
                    }
                } else if (match('*')) {
                    while (!isAtEnd()) {
                        if (peek() == '\n') {
                            line++;
                        }

                        advance();

                        if (peek() == '*' && peekNext() == '/') {
                            advance();
                            advance();
                            break;
                        }
                    }
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            // When encountering whitespace, we simply go back to the beginning of the scan loop
            // that starts new lexeme after the whitespace character
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;
            // When encountering newlines we do the same thing as for whitespace, but we also increase the line counter
            case '\n':
                line++;
                break;
            // Handling string literals
            case '"': string(); break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);

        if (type == null) type = TokenType.IDENTIFIER;

        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the `,`
            advance();

            while (isDigit(peek())) advance();
        }

        // Add number token, convert lexeme to numeric value
        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Consumes character until it hits `"` that ends the string.
     * If we don't hit the closing `"` we raise an error.
     */
    private void string() {
        // consumes characters until the end of the string, end = hitting the closing " character
        while (peek() != '"' && !isAtEnd()) {
            // we support multiline comments, we need to increase line number
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // The closing of a started string "
        advance();

        // Trim the surrounding quotes
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    /**
     * Matches current character in the lexeme and consumes it if it matches expected, increases current counter
     *
     * @param expected next character we are expecting to match
     * @return
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    /**
     * Lookahead
     * @return
     */
    private char peek() {
        if (isAtEnd()) return '\0';

        return source.charAt(current);
    }

    /**
     * Lookahead
     * @return
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';

        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Consumes the next character in the source file and returns it
     *
     * @return next character in the source file
     */
    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Grabs the text of the current lexeme and creates a new token for it
     *
     * @param type
     * @param literal
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
