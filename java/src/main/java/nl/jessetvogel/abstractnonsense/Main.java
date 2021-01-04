package nl.jessetvogel.abstractnonsense;

import nl.jessetvogel.abstractnonsense.core.Session;
import nl.jessetvogel.abstractnonsense.parser.*;

public class Main {

    public static void main(String[] args) {
        // Read arguments
        boolean json = false;
        for(String arg : args) {
            if(arg.equals("--json"))
                json = true;
        }

        // Create session
        Session session = new Session();

        // Parse input
        while (true) {
            // Read from System.in and write to System.out
            Parser parser = new Parser(System.in, System.out, session);
            if(json)
                parser.setOutputFormat(Formatter.OutputFormat.JSON);
            if (!parser.parse())
                break;
        }
//        catch (ParserException e) {
//                System.err.print("\u26A0\uFE0F Parsing error on line " + e.token.line + " at position " + e.token.position + ": " + e.getMessage());
//            } catch (LexerException e) {
//                System.err.print(" Lexing error: " + e.getMessage());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
    }
}
