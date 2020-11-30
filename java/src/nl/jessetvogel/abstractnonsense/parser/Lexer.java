package nl.jessetvogel.abstractnonsense.parser;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class Lexer {

    public static final List<String> KEYWORDS = List.of("equalities", "exit", "import", "id", "dom", "cod", "cat", "let", "assume", "prove", "apply", "property", "theorem", "use", "with", "then", "def", "exists", "check", "debug");
    public static final List<String> SEPARATORS = List.of("(", ")", "{", "}", "=", ".", ",", ":", "->", "~>", "=>", "&", "|", ";");
    public static final Pattern PATTERN_NUMBERS = Pattern.compile("^\\d+$");
    public static final Pattern PATTERN_IDENTIFIERS = Pattern.compile("^\\w+$");
    public static final Pattern PATTERN_STRING = Pattern.compile("^\"[^\"]*\"$");

    Scanner scanner;

    Token currentToken = null;
    String tmp = "";
    int tmpLine = 0, tmpPosition = 1;

    public Lexer(Scanner scanner) {
        this.scanner = scanner;
    }

    Token tokenize(String str) {
        if(KEYWORDS.contains(str))
            return new Token(tmpLine, tmpPosition, Token.Type.KEYWORD, str);
        if(SEPARATORS.contains(str))
            return new Token(tmpLine, tmpPosition, Token.Type.SEPARATOR, str);
        if(str.equals("\n"))
            return new Token(tmpLine, tmpPosition, Token.Type.NEWLINE, str);
        if(PATTERN_NUMBERS.matcher(str).matches())
            return new Token(tmpLine, tmpPosition, Token.Type.NUMBER, str);
        if(PATTERN_IDENTIFIERS.matcher(str).matches())
            return new Token(tmpLine, tmpPosition, Token.Type.IDENTIFIER, str);
        if(PATTERN_STRING.matcher(str).matches())
            return new Token(tmpLine, tmpPosition, Token.Type.STRING, str.substring(1, str.length() - 1));

        return null;
    }

    Token getToken() throws IOException, LexerException {
        // Read characters until a new Token is produced
        while(true) {
            Character c = scanner.get();

            // End of file
            if(c == null) {
                if(tmp.isEmpty())
                    return new Token( tmpLine, tmpPosition, Token.Type.EOF);

                Token token = tokenize(tmp);
                if(token == null)
                    throw new LexerException("Unexpected end of file");
                currentToken = null;
                tmp = "";
                return token;
            }

            // Whitespace: always marks the end of a token (if there currently is one)
            if(c.equals(' ') || c.equals('\t')) {
                if(tmp.isEmpty())
                    continue;

                if(currentToken == null)
                    throw new LexerException("Unknown token '" + tmp + "'");

                Token token = currentToken;
                currentToken = null;
                tmp = "";
                return token;
            }

            // Comments: mark the end of a token (if there currently is one),
            // then continue discarding characters until a newline appears
            if(c.equals('#')) {
                Token token = null;
                if(!tmp.isEmpty()) {
                    if (currentToken == null)
                        throw new LexerException("Unknown token '" + tmp + "'");
                    token = currentToken;
                }

                do {
                    c = scanner.get();
                } while (c != null && !c.equals('\n'));

                tmp = (c == null ? "" : String.valueOf(c));
                tmpLine = scanner.line;
                tmpPosition = scanner.position;
                currentToken = tokenize(tmp);

                return (token != null) ? token : getToken();
            }

            // Try to enlarge the token if possible
            Token token = tokenize(tmp + c);
            if(token != null) {
                currentToken = token;
                if(tmp.isEmpty()) {
                    tmpLine = scanner.line;
                    tmpPosition = scanner.position;
                }
                tmp += c;
                continue;
            }

            // If we also did not succeed before, hope that it will make sense later
            if(currentToken == null) {
                tmp += c;
                continue;
            }

            // Return the last valid token
            token = currentToken;
            tmp = c.toString();
            tmpLine = scanner.line;
            tmpPosition = scanner.position;
            currentToken = tokenize(tmp);
            return token;
        }
    }

}
