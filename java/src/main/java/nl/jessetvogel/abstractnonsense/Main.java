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
            if (!parser.parse(session))
                break;
        }
    }
}
