package nl.jessetvogel.abstractnonsense.parser;

import nl.jessetvogel.abstractnonsense.core.*;
import nl.jessetvogel.abstractnonsense.prover.Exampler;
import nl.jessetvogel.abstractnonsense.prover.Prover;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class Parser {
    private final OutputStream out;
    private final Session session;

    private final Lexer lexer;
    private String filename, directory;
    private Token currentToken;

    private final Formatter formatter;

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

    public Parser(InputStream in, OutputStream out, Session session) {
        this.out = out;
        this.session = session;

        lexer = new Lexer(new Scanner(in));
        directory = "";
        filename = "";
        currentToken = null;

        formatter = new Formatter(session, Formatter.OutputFormat.PLAIN);
    }

    public void setLocation(String directory, String filename) {
        this.directory = directory;
        this.filename = filename;
    }

    public void setOutputFormat(Formatter.OutputFormat format) {
        formatter.format = format;
    }

    // ---- Token methods ----

    private void nextToken() throws IOException, Lexer.LexerException {
        currentToken = lexer.getToken();
        if (currentToken.type == Token.Type.NEWLINE)
            nextToken();
    }

    private boolean found(Token.Type type) throws IOException, Lexer.LexerException {
        return found(type, null);
    }

    private boolean found(Token.Type type, String data) throws IOException, Lexer.LexerException {
        if (currentToken == null)
            nextToken();
        return type == null || (currentToken.type == type && (data == null || data.equals(currentToken.data)));
    }

    private Token consume() throws IOException, Lexer.LexerException, ParserException {
        return consume(null, null);
    }

    private Token consume(Token.Type type) throws IOException, Lexer.LexerException, ParserException {
        return consume(type, null);
    }

    private Token consume(Token.Type type, String data) throws IOException, Lexer.LexerException, ParserException {
        if (found(type, data)) {
            Token token = currentToken;
            currentToken = null;
            return token;
        } else {
            throw new ParserException(currentToken, String.format("Expected %s (%s) but found %s (%s)", data != null ? data : "\b", type, currentToken.data != null ? currentToken.data : "\b", currentToken.type));
        }
    }

    // ---- Parsing methods ----

    public boolean parse(Diagram diagram) {
        try {
            while (!found(Token.Type.EOF)) {
                parseStatement(diagram);
                if (session.contradiction()) {
                    output(formatter.messageContradiction());
                    return false;
                }
            }
            consume(Token.Type.EOF);
        } catch (ParserException e) {
            output(formatter.messageError(e.getMessage()));
        } catch (Lexer.LexerException e) {
            output(formatter.messageError((filename.equals("") ? "" : filename + ":") + e.getMessage()));
        } catch (IOException e) {
            output(formatter.messageError("IOException: " + e.getMessage()));
        }

        return true;
    }

    private void parseStatement(Diagram diagram) throws ParserException, IOException, Lexer.LexerException {
        /* STATEMENT =
                ; |
                exit |
                import STRING |
                check (theorem (* | IDENTIFIER) | example (* | IDENTIFIER) | NUMBER | MORPHISM) |
                let LIST_OF_IDENTIFIERS : TYPE |
                write IDENTIFIER := MORPHISM |
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

            Parser parser = new Parser(new FileInputStream(file), out, session);
            parser.setLocation(file.getAbsoluteFile().getParent() + File.separator, file.getName());
            parser.setOutputFormat(formatter.format);
            parser.parse(diagram);
            return;
        }

        if (found(Token.Type.KEYWORD, "check")) {
            Token tCheck = consume();

            if (found(Token.Type.KEYWORD, "theorem")) {
                consume();

                if(found(Token.Type.SEPARATOR, "*")) {
                    consume();
                    output(formatter.messageList(session.getTheorems().stream().map(thm -> thm.name).collect(Collectors.toList())));
                    return;
                }

                String name = consume(Token.Type.IDENTIFIER).data;
                Theorem theorem = session.getTheorem(name);
                if (theorem == null)
                    throw new ParserException(tCheck, "Theorem not found");
                output(formatter.messageTheorem(theorem));
                return;
            }

            if (found(Token.Type.KEYWORD, "property")) {
                consume();

                if(found(Token.Type.SEPARATOR, "*")) {
                    consume();
                    output(formatter.messageList(session.getProperties().stream().map(property -> property.name + property.context.signature()).collect(Collectors.toList())));
                    return;
                }

                String name = consume(Token.Type.IDENTIFIER).data;
                String signature = parseSignature();
                Property property = session.getProperty(name, signature);
                if (property == null)
                    throw new ParserException(tCheck, "Property not found");
                output(formatter.messageProperty(property));
                return;
            }

            if (found(Token.Type.KEYWORD, "example")) {
                consume();

                if(found(Token.Type.SEPARATOR, "*")) {
                    consume();
                    output(formatter.messageList(session.getExamples().stream().map(example -> example.name).collect(Collectors.toList())));
                    return;
                }

                String name = consume(Token.Type.IDENTIFIER).data;
                Diagram example = session.getExample(name);
                if (example == null)
                    throw new ParserException(tCheck, "Example not found");
                output(formatter.messageDiagram(example));
                return;
            }

            if (found(Token.Type.IDENTIFIER, "session")) {
                consume();
                output(formatter.messageDiagram(session));
                return;
            }

            if (found(Token.Type.NUMBER)) {
                int index = Integer.parseInt(consume().data);
                Morphism f = session.morphismFromIndex(index);
                if (f == null)
                    throw new ParserException(tCheck, "No morphism with index " + index);
                output(formatter.messageMorphism(f));
                return;
            }

            Morphism f = parseMorphism(diagram);
            output(formatter.messageMorphism(f));
            return;
        }

        if (found(Token.Type.KEYWORD, "let")) {
            Token tLet = consume();
            List<String> identifiers = parseListOfIdentifiers();
            for (String i : identifiers) {
                if (diagram.hasSymbol(i))
                    throw new ParserException(tLet, "Symbol " + i + " is already used");
            }
            consume(Token.Type.SEPARATOR, ":");
            MorphismPair type = parseMorphismType(diagram);
            for (String i : identifiers) {
                try {
                    Morphism f = (type.g == null ? session.createObject(diagram, type.f) : session.createMorphism(diagram, type.f, type.g));
                    diagram.assignSymbol(i, f);
                } catch (CreationException e) {
                    e.printStackTrace();
                }
            }
            return;
        }

        if (found(Token.Type.KEYWORD, "assume")) {
            Token tAssume = consume();
            List<Morphism> assumptions = parseListOfMorphisms(diagram);
            for (Morphism P : assumptions) { // TODO: is this safe?
                if (!session.cat(P).equals(session.Prop))
                    throw new ParserException(tAssume, "Assume requires a proposition");
            }

            for (Morphism P : assumptions) { // TODO: is this safe?
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
            Morphism P = parseMorphism(diagram);
            if (!session.cat(P).equals(session.Prop))
                throw new ParserException(tProve, "Prove requires a Proposition");
            Prover prover = new Prover(session, diagram);
            boolean success = prover.prove(P, 10);
            output(formatter.messageProven(success, prover.getProof()));
            prover.detach();
            return;
        }

        if (found(Token.Type.KEYWORD, "apply")) {
            consume();
            Token tIdentifier = consume(Token.Type.IDENTIFIER);
            String name = tIdentifier.data;
            Theorem thm = session.getTheorem(name);
            if (thm == null)
                throw new ParserException(tIdentifier, "Unknown theorem '" + name + "'");
            consume(Token.Type.SEPARATOR, "(");
            Mapping mapping = thm.createMappingFromData(diagram, parseListOfMorphisms(diagram));
            consume(Token.Type.SEPARATOR, ")");

            List<Morphism> result = null;
            if (mapping != null)
                result = thm.apply(mapping);
            if (result == null)
                output(formatter.messageError("Theorem does not apply to given data"));
            else if (!result.isEmpty())
                output(formatter.messageError("The following conditions must be satisfied: " + session.strList(result)));
            return;
        }

        if (found(Token.Type.KEYWORD, "property")) {
            Token tProperty = consume();

            if(diagram != session)
                throw new ParserException(tProperty, "Properties cannot be defined within example");

            List<String> identifiers = parseListOfIdentifiers();

            Context context = new Context(session, session, String.join(",", identifiers));
            consume(Token.Type.SEPARATOR, "{");
            parseImplicits(context);
            parseGivens(context);
            parseWrites(context);
            parseAssumptions(context);

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
            Token tTheorem = consume();

            if(diagram != session)
                throw new ParserException(tTheorem, "Theorems cannot be defined within example");

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

            if(diagram != session)
                throw new ParserException(tExample, "Examples cannot be defined within example");

            String name = consume(Token.Type.IDENTIFIER).data;

            Diagram example = session.getExample(name);
            if (example == null) {
                example = new Diagram(session, session, name);
                session.addExample(name, example);
            }

            consume(Token.Type.SEPARATOR, "{");
            while(!found(Token.Type.SEPARATOR, "}"))
                parseStatement(example);
            consume(Token.Type.SEPARATOR, "}");
            return;
        }

        if (found(Token.Type.KEYWORD, "search")) {
            Token tSearch = consume();

            if(diagram != session)
                throw new ParserException(tSearch, "Cannot search from within example");

            Context context = new Context(session, session, "");
            consume(Token.Type.SEPARATOR, "{");
            parseImplicits(context);
            parseGivens(context);
            parseWrites(context);
            parseAssumptions(context);
            consume(Token.Type.SEPARATOR, "}");

            Exampler ex = new Exampler(session, context);
            output(formatter.messageExamples(ex.search()));
            context.detach();
            return;
        }

        if(found(Token.Type.KEYWORD, "write")) {
            Token tWrite = consume();
            String name = consume(Token.Type.IDENTIFIER).data;
            if(diagram.hasSymbol(name))
                throw new ParserException(tWrite, "Symbol " + name + " is already used");
            consume(Token.Type.SEPARATOR, ":=");
            Morphism f = parseMorphism(diagram);
            diagram.assignSymbol(name, f);
            return;
        }

        throw new ParserException(currentToken, "Unable to parse statement '" + currentToken.data + "'");
    }

    private void parseImplicits(Diagram diagram) throws ParserException, IOException, Lexer.LexerException {
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
            MorphismPair type = parseMorphismType(diagram);
            for (String sym : identifiers) {
                try {
                    Morphism f = (type.g == null ? session.createObject(diagram, type.f) : session.createMorphism(diagram, type.f, type.g));
                    diagram.assignSymbol(sym, f);
                } catch (CreationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void parseGivens(Diagram diagram) throws ParserException, IOException, Lexer.LexerException {
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
            MorphismPair type = parseMorphismType(diagram);
            for (String sym : identifiers) {
                try {
                    Morphism f = (type.g == null ? session.createObject(diagram, type.f) : session.createMorphism(diagram, type.f, type.g));
                    if (diagram instanceof Context)
                        ((Context) diagram).data.add(f);
                    diagram.assignSymbol(sym, f);
                } catch (CreationException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void parseWrites(Diagram diagram) throws ParserException, IOException, Lexer.LexerException {
        /*  WRITES =
                write IDENTIFIER := MORPHISM ( , IDENTIFIER = MORPHISM )*
         */

        boolean first = true;
        while (first ? found(Token.Type.KEYWORD, "write") : found(Token.Type.SEPARATOR, ",")) {
            first = false;

            consume();
            Token tIdentifier = consume(Token.Type.IDENTIFIER);
            String name = tIdentifier.data;
            if (diagram.hasSymbol(name))
                throw new ParserException(tIdentifier, "Symbol " + name + " is already used");
            consume(Token.Type.SEPARATOR, ":=");
            Morphism f = parseMorphism(diagram);
            diagram.assignSymbol(name, f);
        }
    }

    private void parseConditions(Theorem theorem) throws ParserException, IOException, Lexer.LexerException {
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

    private void parseAssumptions(Diagram diagram) throws ParserException, IOException, Lexer.LexerException {
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

    private void parseConclusions(Theorem theorem) throws ParserException, IOException, Lexer.LexerException {
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

    private List<String> parseListOfIdentifiers() throws ParserException, IOException, Lexer.LexerException {
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

    private Morphism parseMorphism(Diagram diagram) throws ParserException, IOException, Lexer.LexerException {
        return parseMorphism(diagram, MorphismOperator.NONE);
    }

    private Morphism parseMorphism(Diagram diagram, MorphismOperator op) throws ParserException, IOException, Lexer.LexerException {
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
                    throw new ParserException(tIdentifier, "Unknown identifier '" + name + "'");
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
            throw new ParserException(currentToken, "Expected a morphism");

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
                Token t = consume();
                Morphism g = parseMorphism(diagram, MorphismOperator.COMPOSITION);
                List<Morphism> fList = new ArrayList<>();
                fList.add(f);
                fList.add(g);
                try {
                    f = diagram.morphism(Representation.composition(fList));
                } catch (CreationException e) {
                    throw new ParserException(t, e.getMessage());
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

            if (found(Token.Type.SEPARATOR, "->") && op.compare(MorphismOperator.HOM) && session.cat(f).equals(session.Prop)) {
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

    private List<Morphism> parseListOfMorphisms(Diagram diagram) throws ParserException, IOException, Lexer.LexerException {
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

    private MorphismPair parseMorphismType(Diagram diagram) throws ParserException, IOException, Lexer.LexerException {
        Morphism f = parseMorphism(diagram);
        Morphism g;
        if(found(Token.Type.SEPARATOR, "->")) {
            consume();
            g = parseMorphism(diagram);
        }
        else {
            g = null;
        }
        return new MorphismPair(f, g);
    }

    private String parseSignature() throws ParserException, IOException, Lexer.LexerException {
        StringJoiner sj = new StringJoiner(",", "(", ")");
        consume(Token.Type.SEPARATOR, "(");

        sj.add(String.valueOf(Integer.parseInt(consume(Token.Type.NUMBER).data)));
        while (found(Token.Type.SEPARATOR, ",")) {
            consume();
            sj.add(String.valueOf(Integer.parseInt(consume(Token.Type.NUMBER).data)));
        }

        consume(Token.Type.SEPARATOR, ")");
        return sj.toString();
    }

    private void output(String message) {
        try {
            out.write(message.getBytes(StandardCharsets.UTF_8));
            out.write('\n');
        } catch (IOException ignored) {
        }
    }

    private class ParserException extends Exception {

        private final Token token;

        public ParserException(Token token, String message) {
            super(message);
            this.token = token;
        }

        public String getMessage() {
            if (!filename.equals(""))
                return filename + ":" + token.line + ":" + token.position + ": " + super.getMessage();
            return super.getMessage();
        }

    }

}
