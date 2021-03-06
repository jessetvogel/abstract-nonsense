package nl.jessetvogel.abstractnonsense.parser;

import java.io.IOException;
import java.io.InputStream;

class Scanner {

    final InputStream stream;
    int line = 1, column = 0;

    Scanner(InputStream stream) {
        this.stream = stream;
    }

    Character get() throws IOException {
        int r = stream.read();
        if(r == -1)
            return null;

        Character c = (char) r;
        if(c.equals('\n')) {
            line ++;
            column = 0;
        }
        else
            column++;
        return c;
    }

}
