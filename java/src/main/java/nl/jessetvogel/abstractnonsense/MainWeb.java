package nl.jessetvogel.abstractnonsense;

import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.api.Import;
import nl.jessetvogel.abstractnonsense.core.Session;
import nl.jessetvogel.abstractnonsense.parser.*;

import java.io.*;

public class MainWeb {

    public static Session session;
    public static InputStream istream;
    public static Writer writer;

    public static void main(String[] args) throws IOException {
        session = new Session();
        PipedOutputStream ostream = new PipedOutputStream();
        writer = new OutputStreamWriter(ostream);

        // Parse input
        while (true) {
            // Read from ByteArrayInputStream
            istream = new PipedInputStream(ostream);
            Scanner scanner = new Scanner(istream);
            Lexer lexer = new Lexer(scanner);
            Parser parser = new Parser(lexer, session);

            try {
                if (!parser.parse())
                    break;
            } catch (ParserException e) {
                output("\u26A0\uFE0F Parsing error on line " + e.token.line + " at position " + e.token.position + ": " + e.getMessage());
            } catch (LexerException e) {
                output("\u26A0\uFE0F Lexing error: " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Import(module = "module", name = "output")
    public static native void output(String str);

    @Export("input")
    public static void input(String str) {
        try {
            writer.write(str);
        }
        catch (IOException e) {
            output(e.getMessage());
        }
    }

}
