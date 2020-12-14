package nl.jessetvogel.abstractnonsense;

import nl.jessetvogel.abstractnonsense.core.Session;
import nl.jessetvogel.abstractnonsense.parser.*;

import java.io.IOException;

public class MainJar {

    public static void main(String[] args) {
        Session session = new Session();

        // Parse input
        while (true) {
            // Read from System.in
            Scanner scanner = new Scanner(System.in);
            Lexer lexer = new Lexer(scanner);
            Parser parser = new Parser(lexer, session);

            try {
                if (!parser.parse())
                    break;
            } catch (ParserException e) {
                System.err.print("\u26A0\uFE0F Parsing error on line " + e.token.line + " at position " + e.token.position + ": " + e.getMessage());
            } catch (LexerException e) {
                System.err.print("\u26A0\uFE0F Lexing error: " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
