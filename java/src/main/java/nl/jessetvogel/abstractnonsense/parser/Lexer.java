package nl.jessetvogel.abstractnonsense.parser;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class Lexer {

    public static final List<String> KEYWORDS = List.of("exit", "import", "id", "dom", "cod", "cat", "let", "assume", "prove", "apply", "property", "theorem", "example", "search", "use", "with", "then", "write", "def", "exists", "check", "debug", "inspect");
    public static final List<String> SEPARATORS = List.of("(", ")", "{", "}", "=", ".", ",", ":", "->", "=>", "&", "|", "~", ";");
    public static final Pattern PATTERN_NUMBERS = Pattern.compile("^\\d+$");
    public static final Pattern PATTERN_IDENTIFIERS = Pattern.compile("^\\w+$");
    public static final Pattern PATTERN_STRING = Pattern.compile("^\"[^\"]*\"$");

    private final Scanner scanner;

    private Token currentToken = null;
    private final StringBuilder sb;
    private int line = 0, position = 1;

    public Lexer(Scanner scanner) {
        this.scanner = scanner;
        sb = new StringBuilder();
    }

    Token getToken() throws IOException, LexerException {
        // Read characters until a new Token is produced
        while (true) {
            Character c = scanner.get();

            // End of file
            if (c == null) {
                if (sb.length() == 0)
                    return new Token(Token.Type.EOF, null, line, position);

                if (!tokenize(sb.toString()))
                    throw new LexerException("Unexpected end of file");

                return makeToken();
            }

            // Whitespace: always marks the end of a token (if there currently is one)
            if (c.equals(' ') || c.equals('\t')) {
                if (sb.length() == 0)
                    continue;

                return makeToken();
            }

            // Comments: mark the end of a token (if there currently is one),
            // then continue discarding characters until a newline appears
            if (c.equals('#')) {
                Token token = (sb.length() != 0) ? makeToken() : null;

                do {
                    c = scanner.get();
                } while (c != null && !c.equals('\n'));

                if (c != null)
                    sb.append(c);

                line = scanner.line;
                position = scanner.position;
                tokenize(sb.toString());
                return (token != null) ? token : getToken();
            }

            // If string buffer is empty, set line and position of next token
            if (sb.length() == 0) {
                line = scanner.line;
                position = scanner.position;
            }

            // Enlarge the token if possible
            sb.append(c);

            // If can tokenize, just continue
            if (tokenize(sb.toString()))
                continue;

            // If we also did not tokenize before, hope that it will make sense later
            if (currentToken == null)
                continue;

            // Return the last valid token
            Token token = makeToken();
            sb.append(c);
            line = scanner.line;
            position = scanner.position;
            tokenize(sb.toString());
            return token;
        }
    }

    private boolean tokenize(String str) {
        Token.Type type;
        if (KEYWORDS.contains(str))
            type = Token.Type.KEYWORD;
        else if (SEPARATORS.contains(str))
            type = Token.Type.SEPARATOR;
        else if (str.equals("\n"))
            type = Token.Type.NEWLINE;
        else if (PATTERN_NUMBERS.matcher(str).matches())
            type = Token.Type.NUMBER;
        else if (PATTERN_IDENTIFIERS.matcher(str).matches())
            type = Token.Type.IDENTIFIER;
        else if (PATTERN_STRING.matcher(str).matches()) {
            type = Token.Type.STRING;
            str = str.substring(1, str.length() - 1);
        }
        else
            return false;

        if (currentToken == null)
            currentToken = new Token(type, str, line, position);
        else {
            currentToken.type = type;
            currentToken.data = str;
        }
        return true;
    }

    private Token makeToken() throws LexerException {
        if (currentToken == null)
            throw new LexerException("Unknown token '" + sb.toString() + "'");

        Token token = currentToken;
        currentToken = null;
        sb.setLength(0);
        return token;
    }

}
