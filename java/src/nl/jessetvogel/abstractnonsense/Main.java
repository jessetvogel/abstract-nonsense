package nl.jessetvogel.abstractnonsense;

import nl.jessetvogel.abstractnonsense.core.Session;
import nl.jessetvogel.abstractnonsense.core.Morphism;
import nl.jessetvogel.abstractnonsense.core.Property;
import nl.jessetvogel.abstractnonsense.parser.*;

import java.io.FileInputStream;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Session session = new Session();

//        // TODO: REMOVE THIS
//        try {
//            Scanner scanner = new Scanner(new FileInputStream("/Users/jessevogel/Projects/abstract-nonsense/math/math.txt"));
//            Lexer lexer = new Lexer(scanner);
//            (new Parser(lexer, session)).parse();
//        } catch (LexerException | IOException | ParserException e) {
//            e.printStackTrace();
//        }

        // Parse input
        while (true) {
            // Read from System.in
            Scanner scanner = new Scanner(System.in);
            Lexer lexer = new Lexer(scanner);
            Parser parser = new Parser(lexer, session);

            try {
                parser.parse();
            } catch (ParserException e) {
                System.err.print("Parsing error on line " + e.token.line + " at position " + e.token.position + ": " + e.getMessage());
            } catch (LexerException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
