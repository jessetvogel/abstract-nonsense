package nl.jessetvogel.abstractnonsense.parser;

public class ParserException extends Exception {

    Token token;

    ParserException(Token t, String message) {
        super(message);
    }

}
