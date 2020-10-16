package nl.jessetvogel.abstractnonsense.parser;

import java.io.IOException;
import java.io.InputStream;

public class Scanner {

    InputStream stream;
    public int line = 1, position = 0;

    public Scanner(InputStream stream) {
        this.stream = stream;
    }

    Character get() throws IOException {
        int r = stream.read();
        if(r == -1)
            return null;

        Character c = (char) r;
        if(c.equals('\n')) {
            line ++;
            position = 0;
        }
        else
            position ++;
        return c;
    }

}
