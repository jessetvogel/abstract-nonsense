package nl.jessetvogel.abstractnonsense;

import nl.jessetvogel.abstractnonsense.core.Book;
import nl.jessetvogel.abstractnonsense.core.Global;
import nl.jessetvogel.abstractnonsense.core.Morphism;
import nl.jessetvogel.abstractnonsense.core.Property;
import nl.jessetvogel.abstractnonsense.parser.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        {
            // Create Cat
            Global.book = new Book(null);
            Global.Cat = new Morphism();
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
            Morphism x = Global.Equals.createObject(C), y = Global.Equals.createObject(C), z = Global.Equals.createObject(C), w = Global.Equals.createObject(C);
            Global.Equals.addData(Global.Equals.createMorphism(x, y, true));
            Global.Equals.addData(Global.Equals.createMorphism(z, w, true));
            Global.book.addProperty(Global.Equals);
        }

        try {
            Scanner scanner = new Scanner(new FileInputStream("/Users/jessevogel/Projects/abstract-nonsense/math/math.txt"));
            Lexer lexer = new Lexer(scanner);
            (new Parser(lexer, Global.book)).parse();
        } catch (LexerException | IOException | ParserException e) {
            e.printStackTrace();
        }

        // Parse input
        while (true) {
            // Read from System.in
            Scanner scanner = new Scanner(System.in);
            Lexer lexer = new Lexer(scanner);
            Parser parser = new Parser(lexer, Global.book);

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
