package nl.jessetvogel.abstractnonsense;

import nl.jessetvogel.abstractnonsense.core.Book;
import nl.jessetvogel.abstractnonsense.core.Global;
import nl.jessetvogel.abstractnonsense.core.Morphism;
import nl.jessetvogel.abstractnonsense.parser.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
	    // Create Cat, True, False
        Global.book = new Book(null);
        Global.Cat =  new Morphism();
        Global.book.addMorphism(Global.Cat);
        Global.book.assignSymbol("Cat", Global.Cat);
        Global.False = Global.book.createNumber(0);
        Global.True = Global.book.createNumber(1);
        Global.book.assignSymbol("False", Global.False);
        Global.book.assignSymbol("True", Global.True);

        // Read from System.in
        Scanner scanner = new Scanner(System.in);
        Lexer lexer = new Lexer(scanner);
        Parser parser = new Parser(lexer, Global.book);

        try {
            parser.parse();
        }
        catch (LexerException | IOException | ParserException e) {
            e.printStackTrace();
        }
    }
}
