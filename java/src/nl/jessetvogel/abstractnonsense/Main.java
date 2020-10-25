package nl.jessetvogel.abstractnonsense;

import nl.jessetvogel.abstractnonsense.core.Book;
import nl.jessetvogel.abstractnonsense.core.Global;
import nl.jessetvogel.abstractnonsense.core.Morphism;
import nl.jessetvogel.abstractnonsense.core.Property;
import nl.jessetvogel.abstractnonsense.parser.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        {
            // Create Cat
            Global.book = new Book(null);
            Global.Cat =  new Morphism();
            Global.book.addMorphism(Global.Cat);
            Global.book.assignSymbol("Cat", Global.Cat);

            // Create True, False
            Global.False = Global.book.createNumber(0);
            Global.True = Global.book.createNumber(1);
            Global.book.assignSymbol("False", Global.False);
            Global.book.assignSymbol("True", Global.True);

            // Create And, Or, Implies, Equals
            Global.And = new Property(Global.book, "&");
            Global.And.addData(Global.And.createObject(Global.Cat));
            Global.And.addData(Global.And.createObject(Global.Cat));
            Global.book.addProperty(Global.And);

            Global.Or = new Property(Global.book, "|");
            Global.Or.addData(Global.Or.createObject(Global.Cat));
            Global.Or.addData(Global.Or.createObject(Global.Cat));
            Global.book.addProperty(Global.Or);

            Global.Implies = new Property(Global.book, "=>");
            Global.Implies.addData(Global.Implies.createObject(Global.Cat));
            Global.Implies.addData(Global.Implies.createObject(Global.Cat));
            Global.book.addProperty(Global.Implies);

            Global.Equals = new Property(Global.book, "=");
            Morphism C = Global.Equals.createObject(Global.Cat);
            Global.Equals.addData(Global.Equals.createObject(C));
            Global.Equals.addData(Global.Equals.createObject(C));
            Global.book.addProperty(Global.And);
        }

        // Parse input
        while(true) {
            // Read from System.in
            Scanner scanner = new Scanner(System.in);
            Lexer lexer = new Lexer(scanner);
            Parser parser = new Parser(lexer, Global.book);

            try {
                parser.parse();
            } catch (LexerException | IOException | ParserException e) {
                e.printStackTrace();
            }
        }
    }
}
