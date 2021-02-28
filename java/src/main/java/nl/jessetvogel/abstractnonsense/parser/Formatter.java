package nl.jessetvogel.abstractnonsense.parser;

import nl.jessetvogel.abstractnonsense.core.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

public class Formatter {

    private final Session session;

    public enum OutputFormat { PLAIN, JSON }
    public OutputFormat format;

    Formatter(Session session, OutputFormat format) {
        this.session = session;
        this.format = format;
    }

    public String messageContradiction() {
        if(format == OutputFormat.PLAIN)
            return "\u26A1 Contradiction!";

        if(format == OutputFormat.JSON)
            return "{\"type\":\"contradiction\"}";

        return null;
    }

    public String messageError(String message) {
        if(format == OutputFormat.PLAIN)
            return String.format("\u26A0\uFE0F Error: %s", message);

        if(format == OutputFormat.JSON)
            return String.format("{\"type\":\"error\",\"message\":\"%s\"}", escape(message));

        return null;
    }

    public String messageProven(boolean success, List<String> proof) {
        if(format == OutputFormat.PLAIN) {
            if(success)
                return String.format("\uD83C\uDF89 Proven!\n%s", String.join("\n", proof));
            else
                return "\uD83E\uDD7A Could not prove..";
        }

        if(format == OutputFormat.JSON) {
            StringJoiner sjProof = new StringJoiner(",", "[", "]");
            for(String line : proof)
                sjProof.add("\"" + escape(line) + "\"");
            return String.format("{\"type\":\"proof\",\"success\":%s,\"proof\":%s}", success, sjProof.toString());
        }

        return null;
    }

    public String messageExamples(List<Mapping> examples) {
        if(format == OutputFormat.PLAIN) {
            if(examples.isEmpty())
                return "\uD83E\uDD7A No examples found";

            StringBuilder sb = new StringBuilder();
            for(Mapping m : examples) {
                StringJoiner sjExample = new StringJoiner(", ", "{ ", " }");
                for(Morphism f : m.context.data)
                    sjExample.add(session.str(f) + ": " + session.str(m.map(f)));
                sb.append(sjExample.toString()).append('\n');
            }
            return String.format("\uD83D\uDCD6 Found %d examples!\n%s", examples.size(), sb.toString());
        }

        if (format == OutputFormat.JSON) {
            StringJoiner sjExamples = new StringJoiner(",", "[", "]");
            for(Mapping m : examples) {
                StringJoiner sjExample = new StringJoiner(",", "{", "}");
                for(Morphism f : m.context.data)
                    sjExample.add("\"" + escape(session.str(f)) + "\":\"" + escape(session.str(m.map(f))) + "\"");
                sjExamples.add(sjExample.toString());
            }
            return String.format("{\"type\":\"examples\",\"examples\":%s}", sjExamples.toString());
        }

        return null;
    }

    public String messageMorphism(Morphism f) {
        if(format == OutputFormat.PLAIN)
            return formatMorphism(f);

        if(format == OutputFormat.JSON)
            return String.format("{\"type\":\"morphism\",\"morphism\":{%s}}", formatMorphism(f));

        return null;
    }

    public String messageTheorem(Theorem thm) {
        if(format == OutputFormat.PLAIN) {
            return String.format("Theorem %s:\n\tMorphisms: %s\n\tData: %s\n\tConditions: %s\n\tConclusions: %s",
                    thm.name,
                    formatDiagram(thm),
                    formatMorphismList(thm.data),
                    formatMorphismList(thm.getConditions()),
                    formatMorphismList(thm.getConclusions()));
        }

        if(format == OutputFormat.JSON) {
            return String.format("{\"type\":\"theorem\",\"name\":\"%s\",\"morphisms\":%s,\"data\":%s,\"conditions\":%s,\"conclusions\":%s}",
                    thm.name,
                    formatDiagram(thm),
                    formatMorphismList(thm.data),
                    formatMorphismList(thm.getConditions()),
                    formatMorphismList(thm.getConclusions()));
        }

        return null;
    }

    public String messageProperty(Property property) {
        if(format == OutputFormat.PLAIN) {
            return String.format("Property %s:\n\tMorphisms: %s\n\tData: %s\n\tDefinition: %s",
                    property.name,
                    formatDiagram(property.context),
                    formatMorphismList(property.context.data),
                    property.definition == null ? "None" : session.str(property.definition));
        }

        if(format == OutputFormat.JSON) {
            return String.format("{\"type\":\"property\",\"name\":\"%s\",\"morphisms\":%s,\"data\":%s,\"definition\":%s}",
                    property.name,
                    formatDiagram(property.context),
                    formatMorphismList(property.context.data),
                    property.definition == null ? "null" : ("\"" + escape(session.str(property.definition)) + "\""));
        }

        return null;
    }

    public String messageDiagram(Diagram diagram) {
        if(format == OutputFormat.PLAIN)
            return formatDiagram(diagram);

        if(format == OutputFormat.JSON)
            return String.format("{\"type\":\"diagram\",\"morphisms\":%s}", formatDiagram(diagram));

        return null;
    }

    public String messageList(Collection<String> items) {
        if(format == OutputFormat.PLAIN) {
            StringJoiner sj = new StringJoiner(", ");
            for(String s : items)
                sj.add(s);
            return sj.toString();
        }

        if(format == OutputFormat.JSON) {
            StringJoiner sj = new StringJoiner(",", "[", "]");
            for(String s : items)
                sj.add("\"" + escape(s) + "\"");
            return String.format("{\"type\":\"list\",\"items\":%s}", sj.toString());
        }

        return null;
    }

    // -------- Private formatting methods --------

    private String formatMorphism(Morphism f) {
        String strF = session.str(f);
        String strCat = session.str(session.cat(f));
        String strDom = f.k != 0 ? session.str(session.dom(f)) : null;
        String strCod = f.k != 0 ? session.str(session.cod(f)) : null;

        if(format == OutputFormat.PLAIN)
            return "[" + f.index + "] " + strF + " : " + ((f.k == 0) ? strCat : strDom + " -> " + strCod);

        if(format == OutputFormat.JSON) {
            if(f.k == 0)
                return String.format("\"%s\":{\"k\":%d,\"cat\":\"%s\"}", escape(strF), f.k, escape(strCat));
            else
                return String.format("\"%s\":{\"k\":%d,\"cat\":\"%s\",\"dom\":\"%s\",\"cod\":\"%s\"}", escape(strF), f.k, escape(strCat), escape(strDom), escape(strCod));
        }

        return null;
    }

    private String formatMorphismList(List<Morphism> list) {
        StringJoiner sj = (format == OutputFormat.JSON) ? new StringJoiner(",", "[", "]") : new StringJoiner(", ");
        for(Morphism f : list) {
            String strF = session.str(f);
            if(format == OutputFormat.JSON)
                sj.add("\"" + escape(strF) + "\"");
            else
                sj.add(strF);
        }
        return sj.toString();
    }

    private String formatDiagram(Diagram diagram) {
        StringJoiner sj = (format == OutputFormat.JSON) ? new StringJoiner(",", "{", "}") : new StringJoiner(", ");
        for(int index : new ArrayList<>(diagram.indices)) {
            if(session.nCat.contains(index))
                continue;
            sj.add(formatMorphism(session.morphismFromIndex(index)));
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
