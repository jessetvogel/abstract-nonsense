package nl.jessetvogel.abstractnonsense.parser;

import nl.jessetvogel.abstractnonsense.core.*;
import nl.jessetvogel.abstractnonsense.prover.Prover;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final Session session;

    private final Lexer lexer;
    private Token currentToken;

    public Parser(Lexer lexer, Session session) {
        this.lexer = lexer;
        this.session = session;
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
            parseStatement(session);
        consume(Token.Type.EOF);
    }

    // -------- Parse Functions ------------

    private void parseStatement(Session session) throws ParserException, IOException, LexerException {
        /* STATEMENT =
                ; |
                exit |
                import STRING |
                let LIST_OF_IDENTIFIERS : TYPE |
                assume LIST_OF_MORPHISMS |
                prove MORPHISM |
                apply IDENTIFIER ( LIST_OF_MORPHISMS ) |
                property LIST_OF_IDENTIFIERS { IMPLICITS GIVENS (def MORPHISM)? } |
                theorem IDENTIFIER { IMPLICITS GIVENS CONDITIONS CONCLUSIONS } |
                check MORPHISM
         */

        if (found(Token.Type.SEPARATOR, ";")) {
            consume();
            return;
        }

//        if(found(Token.Type.KEYWORD, "equalities")) {
//            consume();
//            try {
//                session.resolveEqualities();
//            } catch (CreationException e) {
//                e.printStackTrace();
//            }
//            return;
//        }

        if (found(Token.Type.KEYWORD, "exit")) {
            consume();
            System.exit(0);
            return;
        }

        if(found(Token.Type.KEYWORD, "import")) {
            consume();
            String path = consume(Token.Type.STRING).data;
            Scanner scanner = new Scanner(new FileInputStream(path));
            Lexer lexer = new Lexer(scanner);
            (new Parser(lexer, session)).parse();
            return;
        }

        if (found(Token.Type.KEYWORD, "let")) {
            Token tLet = consume();
            List<String> identifiers = parseListOfIdentifiers();
            for (String i : identifiers) {
                if (session.hasSymbol(i))
                    throw new ParserException(tLet, "Name " + i + " has already been used");
            }
            consume(Token.Type.SEPARATOR, ":");
            Morphism type = parseMorphism(session);
            for (String i : identifiers) {
                try {
                    session.assignSymbol(i, session.createObject(session, type));
                } catch (CreationException e) {
                    e.printStackTrace();
                }
            }
            return;
        }

        if (found(Token.Type.KEYWORD, "assume")) {
            Token tAssume = consume();
            List<Morphism> list = parseListOfMorphisms(session);
            for(Morphism P : list) {
                if (!session.cat(P).equals(session.Prop))
                    throw new ParserException(tAssume, "Assume requires a Proposition");
                try {
                    session.identify(P, session.True);
                }
                catch(Exception e) {
                    throw new ParserException(tAssume, "Failed to assume: " + e.getMessage());
                }
            }
            return;
        }

        if (found(Token.Type.KEYWORD, "prove")) {
            consume();
            Token tProve = currentToken;
            Morphism P = parseMorphism(session);
            if (!session.cat(P).equals(session.Prop))
                throw new ParserException(tProve, "Prove requires a Proposition");
            Prover prover = new Prover(session, session);
            if (prover.prove(P, 10))
                System.out.println("Proven!");
            else
                System.out.println("Could not prove");
            return;
        }

        if (found(Token.Type.KEYWORD, "apply")) {
            consume();
            Token tIdentifier = consume(Token.Type.IDENTIFIER);
            String name = tIdentifier.data;
            Theorem thm = session.getTheorem(name);
            if (thm == null)
                throw new ParserException(tIdentifier, "Unknown theorem " + name);
            consume(Token.Type.SEPARATOR, "(");
            Mapping mapping = thm.mappingFromData(session, parseListOfMorphisms(session));
            consume(Token.Type.SEPARATOR, ")");

            List<Morphism> result = thm.apply(mapping);
            if (result == null)
                System.out.println("Could not apply theorem");
            else if (result.isEmpty())
                System.out.println("Theorem applied successfully");
            else
                System.out.println("The following conditions must be satisfied: " + session.strList(result));

            return;
        }

        if (found(Token.Type.KEYWORD, "property")) {
            Token tProperty = consume();
            List<String> identifiers = parseListOfIdentifiers();

            Context context = new Context(session, session);
            consume(Token.Type.SEPARATOR, "{");
            parseImplicits(context);
            parseGivens(context);

            Morphism definition = null;
            if(found(Token.Type.KEYWORD, "def")) {
                consume();
                definition = parseMorphism(context);
            }

            consume(Token.Type.SEPARATOR, "}");

            if(!context.isReduced())
                throw new ParserException(tProperty, "Property context contains morphisms that do not depend on the data");

            String signature = context.signature();
            for(String name : identifiers) {
                if(session.hasProperty(name, signature))
                    throw new ParserException(tProperty, "Property " + name + " with signature " + signature + " already used");

                Property property = new Property(context, name, definition);
                session.addProperty(property);
            }

            return;
        }

        if (found(Token.Type.KEYWORD, "theorem")) {
            consume();
            Token tIdentifier = consume(Token.Type.IDENTIFIER);
            String name = tIdentifier.data;
            if (session.hasTheorem(name))
                throw new ParserException(tIdentifier, "Theorem name " + name + " already used");
            Theorem thm = new Theorem(session, name);
            consume(Token.Type.SEPARATOR, "{");
            parseImplicits(thm);
            parseGivens(thm);
            parseConditions(thm);
            if (!thm.isReduced())
                throw new ParserException(tIdentifier, "Theorem context contains morphisms that do not depend on the data");
            parseConclusions(thm);
            consume(Token.Type.SEPARATOR, "}");
            session.addTheorem(thm);
            return;
        }

        if (found(Token.Type.KEYWORD, "check")) {
            consume();
            Morphism x = parseMorphism(session);
            if(x.k == 0)
                System.out.printf("%s : %s%n", session.str(x), session.str(session.cat(x)));
            else
                System.out.printf("%s : %s -> %s (%d-morphism in %s)%n", session.str(x), session.str(session.dom(x)), session.str(session.cod(x)), x.k, session.str(session.cat(x)));
            return;
        }

        if (found(Token.Type.KEYWORD, "debug")) {
            List<Integer> list = new ArrayList<>(session.indices);
            for (int index : list) {
                if(session.nCat.contains(index))
                    continue;

                Morphism x = session.morphism(index);
                if(x == null)
                    continue;

                if(x.k == 0)
                    System.out.printf("%s : %s%n", session.str(x), session.str(session.cat(x)));
                else
                    System.out.printf("%s : %s -> %s (%d-morphism in %s)%n", session.str(x), session.str(session.dom(x)), session.str(session.cod(x)), x.k, session.str(session.cat(x)));
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
            List<String> identifiers = parseListOfIdentifiers();
            for (String i : identifiers) {
                if (context.hasSymbol(i))
                    throw new ParserException(t, "Symbol " + i + " is already used");
            }
            consume(Token.Type.SEPARATOR, ":");
            Morphism type = parseMorphism(context);
            for (String i : identifiers) {
                Morphism x = null;
                try {
                    x = session.createObject(context, type);
                } catch (CreationException e) {
                    e.printStackTrace();
                }
                context.assignSymbol(i, x);
            }
        }
    }

    private void parseGivens(Context context) throws ParserException, IOException, LexerException {
        /*  GIVENS =
                let LIST_OF_IDENTIFIERS : TYPE ( , LIST_OF_IDENTIFIERS : TYPE )*
         */

        if (!found(Token.Type.KEYWORD, "let"))
            throw new ParserException(currentToken, "Context must have data");

        boolean first = true;
        while (first ? found(Token.Type.KEYWORD, "let") : found(Token.Type.SEPARATOR, ",")) {
            first = false;

            consume();
            List<String> identifiers = parseListOfIdentifiers();
            Token t = currentToken;
            for (String i : identifiers) {
                if (context.hasSymbol(i))
                    throw new ParserException(t, "Symbol " + i + " is already used");
            }
            consume(Token.Type.SEPARATOR, ":");
            Morphism type = parseMorphism(context);
            for (String i : identifiers) {
                Morphism x = null;
                try {
                    x = session.createObject(context, type);
                } catch (CreationException e) {
                    e.printStackTrace();
                }
                context.addData(x);
                context.assignSymbol(i, x);
            }
        }
    }

    private void parseConditions(Theorem theorem) throws ParserException, IOException, LexerException {
        /*  CONDITIONS =
                with LIST_OF_MORPHISMS
         */

        Token t = consume(Token.Type.KEYWORD, "with");
        for(Morphism P : parseListOfMorphisms(theorem)) {
            if (!session.cat(P).equals(session.Prop))
                throw new ParserException(t, "Condition must be a Proposition");
            theorem.addCondition(P);
        }
    }

    private void parseConclusions(Theorem theorem) throws ParserException, IOException, LexerException {
        /*  CONCLUSIONS =
                then LIST_OF_MORPHISMS
         */

        Token t = consume(Token.Type.KEYWORD, "then");
        for(Morphism Q : parseListOfMorphisms(theorem)) {
            if (!session.cat(Q).equals(session.Prop))
                throw new ParserException(t, "Conclusion must be a Proposition");
            theorem.addConclusion(Q);
        }
    }

    private List<String> parseListOfIdentifiers() throws ParserException, IOException, LexerException {
        /*  LIST_OF_IDENTIFIERS =
                IDENTIFIER ( , IDENTIFIER )+
         */

        List<String> identifiers = new ArrayList<>();
        identifiers.add(consume(Token.Type.IDENTIFIER).data);
        while (found(Token.Type.SEPARATOR, ",")) {
            consume();
            identifiers.add(consume(Token.Type.IDENTIFIER).data);
        }
        return identifiers;
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
                MORPHISM -> MORPHISM |
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
            try {
                x = session.id(parseMorphism(diagram));
            } catch (CreationException e) {
                throw new ParserException(tId, e.getMessage());
            }
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "dom")) {
            consume();
            consume(Token.Type.SEPARATOR, "(");
            x = session.dom(parseMorphism(diagram));
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "cod")) {
            consume();
            consume(Token.Type.SEPARATOR, "(");
            x = session.cod(parseMorphism(diagram));
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "cat")) {
            consume();
            consume(Token.Type.SEPARATOR, "(");
            x = session.cat(parseMorphism(diagram));
            consume(Token.Type.SEPARATOR, ")");
//        } else if (found(Token.Type.NUMBER)) {
//            Token tNumber = consume();
//            x = diagram.createNumber(Integer.parseInt(tNumber.data));
        } else if (found(Token.Type.IDENTIFIER)) {
            Token tIdentifier = consume();
            String name = tIdentifier.data;

            // Try to interpret name as a symbol in session
            x = diagram.getMorphism(name);

            // If failed, interpret as property or definition
            if (x == null) {
                if(found(Token.Type.SEPARATOR, "(")) {
                    consume(Token.Type.SEPARATOR, "(");
                    List<Morphism> data = parseListOfMorphisms(diagram);
                    consume(Token.Type.SEPARATOR, ")");

                    String signature = session.signature(data);
                    Property property = session.getProperty(name, signature);

                    if (property == null)
                        throw new ParserException(tIdentifier, "No property " + name + " with signature " + signature);

                    try {
                        x = diagram.createPropertyApplication(property, data);
                    } catch (CreationException e) {
                        throw new ParserException(tIdentifier, e.getMessage());
                    }
                }
                else {
                    throw new ParserException(tIdentifier, "Unknown identifier " + name);
                }
            }
        }

        if (x == null)
            throw new ParserException(currentToken, "Expected an object or morphism");

        // Now we have some x, see if we can (possibly) extend it!
        // This seems to be the solution to left-recursive patterns

        while (true) {
            if (found(Token.Type.SEPARATOR, "(")) {
                Token tBracket = consume();
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
                List<Morphism> fList = new ArrayList<>();
                fList.add(x);
                fList.add(y);
                try {
                    x = diagram.createComposition(fList);
                } catch (CreationException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (found(Token.Type.SEPARATOR, "&") || found(Token.Type.SEPARATOR, "|") || found(Token.Type.SEPARATOR, "->") || found(Token.Type.SEPARATOR, "=")) {
                Token t = consume();
                Morphism y = parseMorphism(diagram);
                try {
                    if (t.data.equals("&"))
                        x = diagram.createAnd(x, y);
                    if (t.data.equals("|"))
                        x = diagram.createOr(x, y);
                    if (t.data.equals("->"))
                        x = diagram.createHom(x, y);
                    if (t.data.equals("="))
                        x = diagram.createEquality(x, y);
                } catch (CreationException e) {
                    throw new ParserException(t, e.getMessage());
                }
                continue;
            }

            break;
        }

        return x;
    }

    private List<Morphism> parseListOfMorphisms(Diagram diagram) throws ParserException, IOException, LexerException {
        /*  LIST_OF_OBJECTS =
                OBJECT { , OBJECT }
         */

        List<Morphism> morphisms = new ArrayList<>();
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
