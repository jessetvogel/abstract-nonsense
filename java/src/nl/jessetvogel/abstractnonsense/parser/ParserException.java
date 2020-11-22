package nl.jessetvogel.abstractnonsense.parser;

public class ParserException extends Exception {

    public final Token token;

    ParserException(Token token, String message) {
        super(message);
        this.token = token;
    }

}
