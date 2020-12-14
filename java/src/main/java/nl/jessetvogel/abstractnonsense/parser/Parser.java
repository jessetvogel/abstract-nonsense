package nl.jessetvogel.abstractnonsense.parser;

import nl.jessetvogel.abstractnonsense.core.*;
import nl.jessetvogel.abstractnonsense.prover.Exampler;
import nl.jessetvogel.abstractnonsense.prover.Prover;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final Session session;
    private final Lexer lexer;
    private Token currentToken;
    private String directory;

    private enum MorphismOperator {
        NONE(0), EQUALITY(1), HOM(2), AND(3), OR(4), COMPOSITION(5), NEGATION(6), FUNCTOR(7);

        private final int precedence;

        MorphismOperator(int p) {
            precedence = p;
        }

        boolean compare(MorphismOperator other) {
            return other.precedence >= precedence;
        }
    }

    public Parser(Lexer lexer, Session session) {
        this.lexer = lexer;
        this.session = session;
        directory = "";
        currentToken = null;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
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
            throw new ParserException(currentToken, String.format("Expected %s (%s) but found %s (%s)", data != null ? data : "\b", type, currentToken.data != null ? currentToken.data : "\b", currentToken.type));
        }
    }

    public boolean parse() throws IOException, LexerException, ParserException {
        while (!found(Token.Type.EOF)) {
            parseStatement(session);
            if (session.contradiction()) {
                session.print("\u26A1 Contradiction!");
                return false;
            }
        }
        consume(Token.Type.EOF);
        return true;
    }

    // -------- Parse Functions ------------

    private void parseStatement(Session session) throws ParserException, IOException, LexerException {
        /* STATEMENT =
                ; |
                exit |
                import STRING |
                check MORPHISM |
                let LIST_OF_IDENTIFIERS : TYPE |
                assume LIST_OF_MORPHISMS |
                prove MORPHISM |
                apply IDENTIFIER ( LIST_OF_MORPHISMS ) |
                property LIST_OF_IDENTIFIERS { IMPLICITS GIVENS (def MORPHISM)? } |
                theorem IDENTIFIER { IMPLICITS GIVENS CONDITIONS CONCLUSIONS } |
                example IDENTIFIER { IMPLICITS GIVENS ASSUMPTIONS } |
                search { IMPLICITS GIVENS ASSUMPTIONS }

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

        if (found(Token.Type.KEYWORD, "import")) {
            Token tImport = consume();
            String filename = consume(Token.Type.STRING).data;

            // Check if absolute or relative path
            File file = new File(filename);
            if (!file.isAbsolute())
                file = new File(directory + filename);
            if (!file.isFile())
                throw new ParserException(tImport, "File '" + filename + "' not found");

            Scanner scanner = new Scanner(new FileInputStream(file));
            Lexer lexer = new Lexer(scanner);
            Parser parser = new Parser(lexer, session);
            parser.setDirectory(file.getAbsoluteFile().getParent() + File.separator);
            parser.parse();
            return;
        }

        if (found(Token.Type.KEYWORD, "check")) {
            consume();
            Morphism f = parseMorphism(session);
            String type;
            if (f.k == 0)
                type = session.str(session.cat(f));
            else
                type = session.str(session.dom(f)) + " -> " + session.str(session.cod(f)) + " (" + f.k + "-morphism in " + session.str(session.cat(f));
            session.print(session.str(f) + " : " + type + " (index " + f.index + ")");
            return;
        }

        if (found(Token.Type.KEYWORD, "let")) {
            Token tLet = consume();
            List<String> identifiers = parseListOfIdentifiers();
            for (String i : identifiers) {
                if (session.hasSymbol(i))
                    throw new ParserException(tLet, "Symbol " + i + " is already used");
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
            List<Morphism> assumptions = parseListOfMorphisms(session);
            for (Morphism P : assumptions) {
                if (!session.cat(P).equals(session.Prop))
                    throw new ParserException(tAssume, "Assume requires a proposition");
            }

            for (Morphism P : assumptions) {
                try {
                    session.identify(P, session.True);
                } catch (CreationException e) {
                    e.printStackTrace();
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
                session.print("\uD83C\uDF89 Proven!");
            else
                session.print("\uD83E\uDD7A Could not prove..");
            prover.detach();
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
            Mapping mapping = thm.createMappingFromData(session, parseListOfMorphisms(session));
            consume(Token.Type.SEPARATOR, ")");

            List<Morphism> result = null;
            if (mapping != null)
                result = thm.apply(mapping);
            if (result == null)
                session.print("\u2757 Theorem does not apply to given data");
            else if (!result.isEmpty())
                session.print("\uD83D\uDCA1 The following conditions must be satisfied: " + session.strList(result));
            return;
        }

        if (found(Token.Type.KEYWORD, "property")) {
            Token tProperty = consume();
            List<String> identifiers = parseListOfIdentifiers();

            Context context = new Context(session, session);
            consume(Token.Type.SEPARATOR, "{");
            parseImplicits(context);
            parseGivens(context);
            parseWrites(context);

            Morphism definition = null;
            if (found(Token.Type.KEYWORD, "def")) {
                consume();
                definition = parseMorphism(context);
            }

            consume(Token.Type.SEPARATOR, "}");

            if (!context.isReduced())
                throw new ParserException(tProperty, "Property context contains morphisms that do not depend on the data");

            String signature = context.signature();
            for (String name : identifiers) {
                if (session.hasProperty(name, signature))
                    throw new ParserException(tProperty, "Property " + name + " with signature " + signature + " already exists");

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
                throw new ParserException(tIdentifier, "Theorem name " + name + " is already used");
            Theorem thm = new Theorem(session, name);
            consume(Token.Type.SEPARATOR, "{");
            parseImplicits(thm);
            parseGivens(thm);
            parseWrites(thm);
            parseConditions(thm);
            if (!thm.isReduced())
                throw new ParserException(tIdentifier, "Theorem context contains morphisms that do not depend on the data");
            parseConclusions(thm);
            consume(Token.Type.SEPARATOR, "}");
            session.addTheorem(thm);
            return;
        }

        if (found(Token.Type.KEYWORD, "example")) {
            Token tExample = consume();
            String name = consume(Token.Type.IDENTIFIER).data;

            if (session.hasExample(name))
                throw new ParserException(tExample, "Example name " + name + " is already used");

            Diagram example = new Diagram(session, session);
            consume(Token.Type.SEPARATOR, "{");
            parseImplicits(example);
            parseGivens(example);
            parseWrites(example);
            parseAssumptions(example);
            consume(Token.Type.SEPARATOR, "}");

            session.addExample(name, example);
            return;
        }

        if (found(Token.Type.KEYWORD, "search")) {
            consume();
            Context context = new Context(session, session);
            consume(Token.Type.SEPARATOR, "{");
            parseImplicits(context);
            parseGivens(context);
            parseWrites(context);
            parseAssumptions(context);
            consume(Token.Type.SEPARATOR, "}");

            Exampler ex = new Exampler(session, context);
            ex.search();
            context.detach();
            return;
        }

        // -- FOR DEBUGGING --

        if (found(Token.Type.KEYWORD, "inspect")) {
            Token tInspect = consume();
            String name = consume(Token.Type.IDENTIFIER).data;

            Diagram diagram = null;
            if (name.equals("session"))
                diagram = session;

            if (diagram == null)
                diagram = session.getExample(name);
            if (diagram == null)
                diagram = session.getTheorem(name);
            if (diagram == null)
                diagram = session.getProperty(name, "(0)").context;
            if (diagram == null)
                throw new ParserException(tInspect, "No results for '" + name + "'");

            List<Integer> list = new ArrayList<>(diagram.indices);
            for (int index : list) {
                // Do not display categories of n-categories
                if (session.nCat.contains(index))
                    continue;

                // Because we do not know which k to expect..
                Morphism f = null;
                int k = -1;
                while (f == null && k < 3)
                    f = session.morphismFromIndex(index, ++k);
                if (f == null)
                    continue;

                if (f.k == 0)
                    session.print("[" + f.index + "] " + diagram.str(f) + " : " + diagram.str(session.cat(f)));
                else
                    session.print("[" + f.index + "] " + diagram.str(f) + " : " + diagram.str(session.dom(f)) + " -> " + diagram.str(session.cod(f)) + " (" + f.k + "-morphism in " + diagram.str(session.cat(f)) + ")");
            }

            consume();
            return;
        }

        throw new ParserException(currentToken, "Unable to parse statement (" + currentToken.data + ")");
    }

    private void parseImplicits(Diagram diagram) throws ParserException, IOException, LexerException {
        /*  IMPLICITS =
                use LIST_OF_IDENTIFIERS : TYPE ( , LIST_OF_IDENTIFIERS : TYPE )*
         */

        boolean first = true;
        while (first ? found(Token.Type.KEYWORD, "use") : found(Token.Type.SEPARATOR, ",")) {
            first = false;

            consume();
            Token t = currentToken;
            List<String> identifiers = parseListOfIdentifiers();
            for (String sym : identifiers) {
                if (diagram.hasSymbol(sym))
                    throw new ParserException(t, "Symbol " + sym + " is already used");
            }
            consume(Token.Type.SEPARATOR, ":");
            Morphism type = parseMorphism(diagram);
            for (String sym : identifiers) {
                Morphism f = null;
                try {
                    f = session.createObject(diagram, type);
                } catch (CreationException e) {
                    e.printStackTrace();
                }
                diagram.assignSymbol(sym, f);
            }
        }
    }

    private void parseGivens(Diagram diagram) throws ParserException, IOException, LexerException {
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
                if (diagram.hasSymbol(i))
                    throw new ParserException(t, "Symbol " + i + " is already used");
            }
            consume(Token.Type.SEPARATOR, ":");
            Morphism type = parseMorphism(diagram);
            for (String sym : identifiers) {
                Morphism f = null;
                try {
                    f = session.createObject(diagram, type);
                } catch (CreationException e) {
                    e.printStackTrace();
                }
                if (diagram instanceof Context)
                    ((Context) diagram).data.add(f);
                diagram.assignSymbol(sym, f);
            }
        }
    }

    private void parseWrites(Diagram diagram) throws ParserException, IOException, LexerException {
        /*  WRITES =
                write IDENTIFIER = MORPHISM ( , IDENTIFIER = MORPHISM )*
         */

        boolean first = true;
        while (first ? found(Token.Type.KEYWORD, "write") : found(Token.Type.SEPARATOR, ",")) {
            first = false;

            consume();
            Token tIdentifier = consume(Token.Type.IDENTIFIER);
            String name = tIdentifier.data;
            if (diagram.hasSymbol(name))
                throw new ParserException(tIdentifier, "Symbol " + name + " is already used");
            consume(Token.Type.SEPARATOR, "=");
            Morphism f = parseMorphism(diagram);
            diagram.assignSymbol(name, f);
        }
    }

    private void parseConditions(Theorem theorem) throws ParserException, IOException, LexerException {
        /*  CONDITIONS =
                with LIST_OF_MORPHISMS
         */

        if (!found(Token.Type.KEYWORD, "with"))
            return;

        Token t = consume();
        for (Morphism P : parseListOfMorphisms(theorem)) {
            if (!session.cat(P).equals(session.Prop))
                throw new ParserException(t, "Condition must be a Proposition");
            theorem.addCondition(P);
        }
    }

    private void parseAssumptions(Diagram diagram) throws ParserException, IOException, LexerException {
        /*  ASSUMPTIONS =
                with LIST_OF_MORPHISMS
         */

        if (!found(Token.Type.KEYWORD, "with"))
            return;
        Token t = consume();

        // We validate all the assumptions first before identifying them with True, because those identifications might affect the morphisms!
        List<Morphism> assumptions = parseListOfMorphisms(diagram);
        for (Morphism P : assumptions) {
            if (!session.cat(P).equals(session.Prop))
                throw new ParserException(t, "Assumption must be a Proposition");
        }

        // Now identify all morphisms with True
        try {
            for (Morphism P : assumptions)
                session.identify(P, session.True);
        } catch (Exception e) {
            throw new ParserException(t, e.getMessage());
        }
    }

    private void parseConclusions(Theorem theorem) throws ParserException, IOException, LexerException {
        /*  CONCLUSIONS =
                then LIST_OF_MORPHISMS
         */

        Token t = consume(Token.Type.KEYWORD, "then");
        for (Morphism Q : parseListOfMorphisms(theorem)) {
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
        return parseMorphism(diagram, MorphismOperator.NONE);
    }

    private Morphism parseMorphism(Diagram diagram, MorphismOperator op) throws ParserException, IOException, LexerException {
        /*  MORPHISM =
                \( MORPHISM \) |
                id \( MORPHISM \) |
                dom \( MORPHISM \) |
                cod \( MORPHISM \) |
                cat \( MORPHISM \) |
                NUMBER |
                IDENTIFIER \( LIST_OF_MORPHISMS \) |
                IDENTIFIER |
                ~ MORPHISM |
                MORPHISM \( LIST_OF_MORPHISMS \) |
                MORPHISM . MORPHISM |
                MORPHISM -> MORPHISM |
                MORPHISM & MORPHISM |
                MORPHISM | MORPHISM |
                MORPHISM = MORPHISM |
                ... more?
         */

        Morphism f = null;

        if (found(Token.Type.SEPARATOR, "(")) {
            consume();
            f = parseMorphism(diagram);
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "id")) {
            Token tId = consume();
            consume(Token.Type.SEPARATOR, "(");
            try {
                f = session.id(parseMorphism(diagram));
            } catch (CreationException e) {
                throw new ParserException(tId, e.getMessage());
            }
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "dom")) {
            consume();
            consume(Token.Type.SEPARATOR, "(");
            f = session.dom(parseMorphism(diagram));
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "cod")) {
            consume();
            consume(Token.Type.SEPARATOR, "(");
            f = session.cod(parseMorphism(diagram));
            consume(Token.Type.SEPARATOR, ")");
        } else if (found(Token.Type.KEYWORD, "cat")) {
            consume();
            consume(Token.Type.SEPARATOR, "(");
            f = session.cat(parseMorphism(diagram));
            consume(Token.Type.SEPARATOR, ")");
//        } else if (found(Token.Type.NUMBER)) {
//            Token tNumber = consume();
//            f = diagram.createNumber(Integer.parseInt(tNumber.data));
        } else if (found(Token.Type.IDENTIFIER)) {
            Token tIdentifier = consume();
            String name = tIdentifier.data;

            // Try to interpret name as a symbol in session
            f = diagram.getMorphism(name);

            // If failed, interpret as property or definition
            if (f == null) {
                if (found(Token.Type.SEPARATOR, "(")) {
                    consume(Token.Type.SEPARATOR, "(");
                    List<Morphism> data = parseListOfMorphisms(diagram);
                    consume(Token.Type.SEPARATOR, ")");

                    String signature = session.signature(data);
                    Property property = session.getProperty(name, signature);

                    if (property == null)
                        throw new ParserException(tIdentifier, "No property " + name + " with signature " + signature);

                    try {
                        f = diagram.morphism(Representation.propertyApplication(property, data));
                    } catch (CreationException e) {
                        throw new ParserException(tIdentifier, e.getMessage());
                    }
                } else {
                    throw new ParserException(tIdentifier, "Unknown identifier " + name);
                }
            }
        } else if (found(Token.Type.SEPARATOR, "~")) {
            Token tNegation = consume();
            Morphism g = parseMorphism(diagram, MorphismOperator.NEGATION);
            if (!session.cat(g).equals(session.Prop))
                throw new ParserException(tNegation, "Can only negate propositions");
            try {
                f = diagram.morphism(Representation.hom(g, session.False));
            } catch (CreationException e) {
                throw new ParserException(tNegation, e.getMessage());
            }
        }

        if (f == null)
            throw new ParserException(currentToken, "Expected an object or morphism");

        // Now we have some f, see if we can (possibly) extend it!
        // This seems to be the solution to left-recursive patterns

        while (true) {
            if (found(Token.Type.SEPARATOR, "(") && op.compare(MorphismOperator.FUNCTOR)) {
                Token tBracket = consume();
                Morphism g = parseMorphism(diagram);
                consume(Token.Type.SEPARATOR, ")");
                try {
                    f = diagram.morphism(Representation.functorApplication(f, g));
                } catch (CreationException e) {
                    throw new ParserException(tBracket, e.getMessage());
                }
                continue;
            }

            if (found(Token.Type.SEPARATOR, ".") && op.compare(MorphismOperator.COMPOSITION)) {
                consume();
                Morphism g = parseMorphism(diagram, MorphismOperator.COMPOSITION);
                List<Morphism> fList = new ArrayList<>();
                fList.add(f);
                fList.add(g);
                try {
                    f = diagram.morphism(Representation.composition(fList));
                } catch (CreationException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (found(Token.Type.SEPARATOR, "&") && op.compare(MorphismOperator.AND)) {
                Token t = consume();
                Morphism g = parseMorphism(diagram, MorphismOperator.AND);
                try {
                    f = diagram.morphism(Representation.and(f, g));
                } catch (CreationException e) {
                    throw new ParserException(t, e.getMessage());
                }
                continue;
            }

            if (found(Token.Type.SEPARATOR, "|") && op.compare(MorphismOperator.OR)) {
                Token t = consume();
                Morphism g = parseMorphism(diagram, MorphismOperator.OR);
                try {
                    f = diagram.morphism(Representation.or(f, g));
                } catch (CreationException e) {
                    throw new ParserException(t, e.getMessage());
                }
                continue;
            }

            if (found(Token.Type.SEPARATOR, "->") && op.compare(MorphismOperator.HOM)) {
                Token t = consume();
                Morphism g = parseMorphism(diagram, MorphismOperator.HOM);
                try {
                    f = diagram.morphism(Representation.hom(f, g));
                } catch (CreationException e) {
                    throw new ParserException(t, e.getMessage());
                }
                continue;
            }

            if (found(Token.Type.SEPARATOR, "=") && op.compare(MorphismOperator.EQUALITY)) {
                Token t = consume();
                Morphism g = parseMorphism(diagram, MorphismOperator.EQUALITY);
                try {
                    f = diagram.morphism(Representation.equality(f, g));
                } catch (CreationException e) {
                    throw new ParserException(t, e.getMessage());
                }
                continue;
            }

            break;
        }

        return f;
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

}
