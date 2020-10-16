package nl.jessetvogel.abstractnonsense.parser;

public class Token {

    enum Type {
        IDENTIFIER,
        KEYWORD,
        SEPARATOR,
        EOF,
        NEWLINE,
        NUMBER
    }

    Type type;
    int line, position;
    String data;

    Token(int line, int position, Type type) {
        this(line, position, type, null);
    }

    Token(int line, int position, Type type, String data) {
        this.line = line;
        this.position = position;
        this.type = type;
        this.data = data;
    }

}
