package nl.jessetvogel.abstractnonsense.parser;

public class ParserException extends Exception {

    Token token;

    ParserException(Token token, String message) {
        super(message);
        this.token = token;
    }

}
