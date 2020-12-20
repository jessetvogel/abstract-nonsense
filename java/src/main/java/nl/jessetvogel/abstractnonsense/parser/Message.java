package nl.jessetvogel.abstractnonsense.parser;

import java.net.URLEncoder;
import java.util.StringJoiner;

public class Message {

    enum Type { INFO, ERROR }

    private final Type type;
    private final String message;

    public Message(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public String toString() {
        return "[" + type + "] " + message;
    }

    public String toJSON() {
        StringJoiner sj = new StringJoiner(",","{","}");
        sj.add("\"type\":\"" + String.valueOf(type).toLowerCase() + "\"");
        sj.add("\"message\":\"" + escape(message) + "\"");
        return sj.toString();
    }

    private String escape(String raw) {
        StringBuilder sb = new StringBuilder();
        for(char ch: raw.toCharArray()) {
            if(ch == '\"')
                sb.append("\\\"");
            else if(ch == '\\')
                sb.append("\\\\");
            else if(ch == '\b')
                sb.append("\\b");
            else if(ch == '\f')
                sb.append("\\f");
            else if(ch == '\n')
                sb.append("\\n");
            else if(ch == '\r')
                sb.append("\\r");
            else if(ch == '\t')
                sb.append("\\t");
            else if(ch >= 0x20 && ch <= 0x7E)
                sb.append(ch);
            else
                sb.append(String.format("\\u%04X", (int) ch));
        }
        return sb.toString();
    }

}
