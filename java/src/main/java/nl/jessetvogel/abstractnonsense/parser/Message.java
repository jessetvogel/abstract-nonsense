package nl.jessetvogel.abstractnonsense.parser;

import java.util.List;
import java.util.StringJoiner;

public class Message {

    enum Type { INFO, ERROR }

    private final Type type;
    private final String message;
    private List<String> proof;
    private List<String> examples;

    public Message(Type type, String message) {
        this.type = type;
        this.message = message;
        this.proof = null;
        this.examples = null;
    }

    public void setProof(List<String> proof) {
        this.proof = proof;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(type).append("] ").append(message).append("\n");
        if(proof != null) {
            sb.append("Proof:\n");
            for (String line : proof)
                sb.append("- ").append(line).append("\n");
        }
        if(examples != null) {
            sb.append("Examples:\n");
            for (String line : examples)
                sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public String toJSON() {
        StringJoiner sj = new StringJoiner(",","{","}");
        sj.add("\"type\":\"" + String.valueOf(type).toLowerCase() + "\"");
        sj.add("\"message\":\"" + escape(message) + "\"");
        if(proof != null) {
            StringJoiner sjProof = new StringJoiner(",", "[", "]");
            for(String line : proof)
                sjProof.add("\"" + escape(line) + "\"");
            sj.add("\"proof\":" + sjProof.toString());
        }
        if(examples != null) {
            StringJoiner sjExamples = new StringJoiner(",", "[", "]");
            for(String line : examples)
                sjExamples.add("\"" + escape(line) + "\"");
            sj.add("\"examples\":" + sjExamples.toString());
        }
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
