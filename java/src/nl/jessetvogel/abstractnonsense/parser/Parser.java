package nl.jessetvogel.abstractnonsense.parser;

import nl.jessetvogel.abstractnonsense.core.*;
import nl.jessetvogel.abstractnonsense.prover.Prover;

import java.io.IOException;
import java.util.ArrayList;

public class Parser {

    private final Lexer lexer;
    private final Book book;
    private Token currentToken;

    public Parser(Lexer lexer, Book book) {
        this.lexer = lexer;
        this.book = book;
        currentToken = null;
    }

    private void nextToken() throws IOException, LexerException {
        currentToken = lexer.getToken();
        if (currentToken.type == Token.Type.NEWLINE)
            nextToken();
    }

    private boolean found(Token.Type type) throws IOException, LexerException {
        return found(type, null);
    }

    private boolean found(Token.Type type, String data) throws IOException, LexerException {
        if (currentToken == null)
            nextToken();
        return type == null || (currentToken.type == type && (data == null || data.equals(currentToken.data)));
    }

    private Token consume() throws IOException, LexerException, ParserException {
        return consume(null, null);
    }

    private Token consume(Token.Type type) throws IOException, LexerException, ParserException {
        return consume(type, null);
    }

    private Token consume(Token.Type type, String data) throws IOException, LexerException, ParserException {
        if (found(type, data)) {
            Token token = currentToken;
            currentToken = null;
            return token;
        } else {
            throw new ParserException(currentToken, String.format("Expected %s (%s) but found %s (%s)", data, type, currentToken.data, currentToken.type));
        }
    }

    public void parse() throws IOException, LexerException, ParserException {
        while (!found(Token.Type.EOF))
            parseStatement(book);
        consume(Token.Type.EOF);
    }

    // -------- Parse Functions ------------

    private void parseStatement(Book book) throws ParserException, IOException, LexerException {
        /* STATEMENT =
                ; |
                exit |
                let LIST_OF_IDENTIFIERS : TYPE |
                assume MORPHISM |
                prove MORPHISM |
                apply IDENTIFIER ( LIST_OF_MORPHISMS ) |
                property IDENTIFIER { IMPLICITS GIVENS } |
                theorem IDENTIFIER { IMPLICITS GIVENS CONDITIONS CONCLUSIONS } |
                whats MORPHISM
         */

        if (found(Token.Type.SEPARATOR, ";")) {
            consume();
            return;
        }

        if (found(Token.Type.KEYWORD, "exit")) {
            consume();
            System.exit(0);
            return;
        }

        if (found(Token.Type.KEYWORD, "let")) {
            Token tLet = consume();
            ArrayList<String> identifiers = parseListOfIdentifiers();
            for (String i : identifiers) {
                if (book.hasSymbol(i))
                    throw new ParserException(tLet, "Name " + i + " has already been used");
            }
            consume(Token.Type.SEPARATOR, ":");
            MorphismType mType = parseType(book);
            if (mType.isObject) {
                for (String i : identifiers)
                    book.assignSymbol(i, book.createObject(mType.category));
            } else {
                for (String i : identifiers)
                    book.assignSymbol(i, book.createMorphism(mType.domain, mType.codomain, mType.covariant));
            }
            return;
        }

        if (found(Token.Type.KEYWORD, "assume")) {
            consume();
            Token tAssume = currentToken;
            Morphism C = parseMorphism(book);
            if (!C.isCategory())
                throw new ParserException(tAssume, "Assume requires a category");
            book.createObject(C);
            return;
        }

        if (found(Token.Type.KEYWORD, "prove")) {
            consume();
            Token tProve = currentToken;
            Morphism C = parseMorphism(book);
            if (!C.isCategory())
                throw new ParserException(tProve, "Prove requires a category");
            Prover prover = new Prover(book);
            if (prover.prove(C, 10))
                System.out.println("Proven!");
            else
                System.out.println("Could not prove");
            return;
        }

        if (found(Token.Type.KEYWORD, "apply")) {
            consume();
            Token tIdentifier = consume(Token.Type.IDENTIFIER);
            String name = tIdentifier.data;
            Theorem thm = book.getTheorem(name);
            if (thm == null)
                throw new ParserException(tIdentifier, "Unknown theorem " + name);
            consume(Token.Type.SEPARATOR, "(");
            Mapping mapping = thm.mappingFromData(book, parseListOfMorphisms(book));
            consume(Token.Type.SEPARATOR, ")");

            ArrayList<Morphism> result = thm.apply(mapping);
            if (result == null)
                System.out.println("Could not apply theorem");
            else if (result.isEmpty())
                System.out.println("Theorem applied successfully");
            else
                System.out.println("The following conditions must be satisfied: " + book.strList(result));

            return;
        }

        if (found(Token.Type.KEYWORD, "property")) {
            consume();
            Token tIdentifier = consume(Token.Type.IDENTIFIER);
            String name = tIdentifier.data;
            if (book.hasProperty(name))
                throw new ParserException(tIdentifier, "Property name " + name + " already used");
            Property property = new Property(book, name);
            consume(Token.Type.SEPARATOR, "{");
            parseImplicits(property);
            parseGivens(property);
            consume(Token.Type.SEPARATOR, "}");
            if (!property.isReduced())
                throw new ParserException(tIdentifier, "Property context contains morphisms that do not depend on the data");
            book.addProperty(property);
            return;
        }

        if (found(Token.Type.KEYWORD, "theorem")) {
            consume();
            Token tIdentifier = consume(Token.Type.IDENTIFIER);
            String name = tIdentifier.data;
            if (book.hasTheorem(name))
                throw new ParserException(tIdentifier, "Theorem name " + name + " already used");
            Theorem thm = new Theorem(book, name);
            consume(Token.Type.SEPARATOR, "{");
            parseImplicits(thm);
            parseGivens(thm);
            parseConditions(thm);
            if (!thm.isReduced())
                throw new ParserException(tIdentifier, "Theorem context contains morphisms that do not depend on the data");
            parseConclusions(thm.conclusion);
            consume(Token.Type.SEPARATOR, "}");
            book.addTheorem(thm);
            return;
        }

        if (found(Token.Type.KEYWORD, "whats")) {
            consume();
            Morphism x = parseMorphism(book);
            if (x.isObject())
                System.out.printf("%s : %s%n", book.str(x), book.str(x.category));
            else
                System.out.printf("%s : %s %s %s%n", book.str(x), book.str(x.domain), (x.isFunctor() && !x.covariant) ? "~>" : "->", book.str(x.codomain));
//            System.out.printf("Hash: %s", String.valueOf(x.hashCode()));
            return;
        }

        if (found(Token.Type.KEYWORD, "debug")) {

            for (Morphism x : book.morphisms) {
                if (x.isObject())
                    System.out.printf("%s : %s%n", book.str(x), book.str(x.category));
                else
                    System.out.printf("%s : %s %s %s%n", book.str(x), book.str(x.domain), (x.isFunctor() && !x.covariant) ? "~>" : "->", book.str(x.codomain));
            }

            consume();
            return;
        }

        throw new ParserException(currentToken, "Unable to parse statement (" + currentToken.data + ")");
    }

    private void parseImplicits(Context context) throws ParserException, IOException, LexerException {
        /*  IMPLICITS =
                use LIST_OF_IDENTIFIERS : TYPE ( , LIST_OF_IDENTIFIERS : TYPE )*
         */

        boolean first = true;
        while (first ? found(Token.Type.KEYWORD, "use") : found(Token.Type.SEPARATOR, ",")) {
            first = false;

            consume();
            Token t = currentToken;
            ArrayList<String> identifiers = parseListOfIdentifiers();
            for (String i : identifiers) {
                if (context.hasSymbol(i))
                    throw new ParserException(t, "Symbol " + i + " is already used");
            }
            consume(Token.Type.SEPARATOR, ":");
            MorphismType mType = parseType(context);
            if (mType.isObject) {
                for (String i : identifiers) {
                    Morphism x = context.createObject(mType.category);
                    context.assignSymbol(i, x);
                }
            } else {
                for (String i : identifiers) {
                    Morphism x = context.createMorphism(mType.domain, mType.codomain, mType.covariant);
                    context.assignSymbol(i, x);
                }
            }
        }
    }

    private void parseGivens(Context context) throws ParserException, IOException, LexerException {
        /*  GIVENS =
                for LIST_OF_IDENTIFIERS : TYPE ( , LIST_OF_IDENTIFIERS : TYPE )*
         */

        if (!found(Token.Type.KEYWORD, "for"))
            throw new ParserException(currentToken, "Context must have data");

        boolean first = true;
        while (first ? found(Token.Type.KEYWORD, "for") : found(Token.Type.SEPARATOR, ",")) {
            first = false;

            consume();
            ArrayList<String> identifiers = parseListOfIdentifiers();
            Token t = currentToken;
            for (String i : identifiers) {
                if (context.hasSymbol(i))
                    throw new ParserException(t, "Symbol " + i + " is already used");
            }
            consume(Token.Type.SEPARATOR, ":");
            MorphismType mType = parseType(context);
            if (mType.isObject) {
                for (String i : identifiers) {
                    Morphism x = context.createObject(mType.category);
                    context.addData(x);
                    context.assignSymbol(i, x);
                }
            } else {
                for (String i : identifiers) {
                    Morphism x = context.createMorphism(mType.domain, mType.codomain, mType.covariant);
                    context.addData(x);
                    context.assignSymbol(i, x);
                }
            }
        }
    }

    private void parseConditions(Theorem theorem) throws ParserException, IOException, LexerException {
        /*  CONDITIONS =
                with MORPHISM, ( MORPHISM )*
         */

        boolean first = true;
        while (first ? found(Token.Type.KEYWORD, "with") : found(Token.Type.SEPARATOR, ",")) {
            first = false;

            consume();
            Token t = currentToken;
            Morphism C = parseMorphism(theorem);
            if (!C.isCategory())
                throw new ParserException(t, "Condition must be a category");
            theorem.addCondition(C);
        }
    }

    private void parseConclusions(Diagram conclusion) throws ParserException, IOException, LexerException {
        /*  CONCLUSIONS =
                then ... ( , exists LIST_OF_IDENTIFIERS : TYPE | MORPHISM )*
         */

        boolean first = true;
        while (first ? found(Token.Type.KEYWORD, "then") : found(Token.Type.SEPARATOR, ",")) {
            consume();
            Token t = currentToken;
            if (found(Token.Type.KEYWORD, "exists")) {
                consume();
                ArrayList<String> identifiers = parseListOfIdentifiers();
                consume(Token.Type.SEPARATOR, ":");
                MorphismType mType = parseType(conclusion);
                if (mType.isObject) {
                    for (String i : identifiers)
                        conclusion.assignSymbol(i, conclusion.createObject(mType.category));
                } else {
                    for (String i : identifiers)
                        conclusion.assignSymbol(i, conclusion.createMorphism(mType.domain, mType.codomain, mType.covariant));
                }
            } else {
                Morphism C = parseMorphism(conclusion);
                if (!C.isCategory())
                    throw new ParserException(t, "Conclusion must be a category");
                conclusion.createObject(C);
            }
        }
    }

    private ArrayList<String> parseListOfIdentifiers() throws ParserException, IOException, LexerException {
        /*  LIST_OF_IDENTIFIERS =
                IDENTIFIER ( , IDENTIFIER )+
         */

        ArrayList<String> identifiers = new ArrayList<>();
        identifiers.add(consume(Token.Type.IDENTIFIER).data);
        while (found(Token.Type.SEPARATOR, ",")) {
            consume();
            identifiers.add(consume(Token.Type.IDENTIFIER).data);
        }
        return identifiers;
    }

    private MorphismType parseType(Diagram diagram) throws ParserException, IOException, LexerException {
        /*  TYPE =
                MORPHISM |
                MORPHISM -> MORPHISM |
                MORPHISM ~> MORPHISM
         */

        Morphism X = parseMorphism(diagram);
        boolean isArrow = false, covariant = true;
        if (found(Token.Type.SEPARATOR, "->")) {
            isArrow = true;
            covariant = true;
        }
        if (found(Token.Type.SEPARATOR, "~>")) {
            isArrow = true;
            covariant = false;
        }
        if (isArrow) {
            consume();
            Morphism Y = parseMorphism(diagram);
            return new MorphismType(X, Y, covariant);
        } else {
            return new MorphismType(X);
        }
    }

    private Morphism parseMorphism(Diagram diagram) throws ParserException, IOException, LexerException {
        /*  MORPHISM =
                \( MORPHISM \) |
                id \( MORPHISM \) |
                dom \( MORPHISM \) |
                cod \( MORPHISM \) |
                cat \( MORPHISM \) |
                NUMBER |
                IDENTIFIER \( LIST_OF_MORPHISMS \) |
                IDENTIFIER |
                MORPHISM \( LIST_OF_MORPHISMS \) |
                MORPHISM . MORPHISM |
                MORPHISM => MORPHISM |
                MORPHISM & MORPHISM |
                MORPHISM | MORPHISM |
                MORPHISM = MORPHISM |
                ... more?
         */

        Morphism x = null;

        if (found(Token.Type.SEPARATOR, "(")) {
            consume();
            x = parseMorphism(diagram);
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "id")) {
            Token tId = consume();
            consume(Token.Type.SEPARATOR, "(");
            x = parseMorphism(diagram);
            if (!x.isObject())
                throw new ParserException(tId, "id can only be applied to objects");
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "dom")) {
            consume();
            consume(Token.Type.SEPARATOR, "(");
            x = parseMorphism(diagram).domain;
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "cod")) {
            consume();
            consume(Token.Type.SEPARATOR, "(");
            x = parseMorphism(diagram).codomain;
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "cat")) {
            consume();
            consume(Token.Type.SEPARATOR, "(");
            x = parseMorphism(diagram).category;
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.NUMBER)) {
            Token tNumber = consume();
            x = diagram.createNumber(Integer.parseInt(tNumber.data));
        } else if (found(Token.Type.IDENTIFIER)) {
            Token tIdentifier = consume();
            String name = tIdentifier.data;

            // If name refers to a property
            Property property = diagram.getProperty(name);
            if (property != null) {
                consume(Token.Type.SEPARATOR, "(");
                ArrayList<Morphism> data = parseListOfMorphisms(diagram);
                consume(Token.Type.SEPARATOR, ")");
                try {
                    x = diagram.createPropertyApplication(property, data);
                } catch (CreationException e) {
                    throw new ParserException(tIdentifier, e.getMessage());
                }
            } else {
                // Otherwise get morphism by name
                x = diagram.getMorphism(name);
                if (x == null)
                    throw new ParserException(tIdentifier, "Unknown identifier " + name);
            }
        }

        if (x == null)
            throw new ParserException(currentToken, "Expected an object or morphism");

        // Now we have some x, see if we can (possibly) extend it!
        // This seems to be the solution to left-recursive patterns

        while (true) {
            if (found(Token.Type.SEPARATOR, "(")) {
                Token tBracket = consume();
                if (!x.isFunctor())
                    throw new ParserException(tBracket, "Unexpected '(' as morphism is not a functor");
                Morphism y = parseMorphism(diagram);
                consume(Token.Type.SEPARATOR, ")");
                try {
                    x = diagram.createFunctorApplication(x, y);
                } catch (CreationException e) {
                    throw new ParserException(tBracket, e.getMessage());
                }
                continue;
            }

            if (found(Token.Type.SEPARATOR, ".")) {
                consume();
                Morphism y = parseMorphism(diagram);
                ArrayList<Morphism> fList = new ArrayList<>();
                fList.add(x);
                fList.add(y);
                x = diagram.createComposition(fList);
                continue;
            }

            if (found(Token.Type.SEPARATOR, "&") || found(Token.Type.SEPARATOR, "|") || found(Token.Type.SEPARATOR, "=>") || found(Token.Type.SEPARATOR, "=")) {
                Token t = consume();
                Morphism y = parseMorphism(diagram);
                ArrayList<Morphism> data = new ArrayList<>();
                data.add(x);
                data.add(y);
                try {
                    Property property = null;
                    if (t.data.equals("&"))
                        property = Global.And;
                    if (t.data.equals("|"))
                        property = Global.Or;
                    if (t.data.equals("=>"))
                        property = Global.Implies;
                    if (t.data.equals("="))
                        property = Global.Equals;
                    x = diagram.createPropertyApplication(property, data);
                } catch (CreationException e) {
                    throw new ParserException(t, e.getMessage());
                }
                continue;
            }

            break;
        }

        return x;
    }

    private ArrayList<Morphism> parseListOfMorphisms(Diagram diagram) throws ParserException, IOException, LexerException {
        /*  LIST_OF_OBJECTS =
                OBJECT { , OBJECT }
         */

        ArrayList<Morphism> morphisms = new ArrayList<>();
        morphisms.add(parseMorphism(diagram));

        while (found(Token.Type.SEPARATOR, ",")) {
            consume();
            morphisms.add(parseMorphism(diagram));
        }

        return morphisms;
    }

    private static class MorphismType {

        boolean isObject;
        Morphism category, domain, codomain;
        boolean covariant;

        MorphismType(Morphism category) {
            isObject = true;
            this.category = category;
        }

        MorphismType(Morphism domain, Morphism codomain, boolean covariant) {
            isObject = false;
            this.domain = domain;
            this.codomain = codomain;
            this.covariant = covariant;
        }

    }
}
