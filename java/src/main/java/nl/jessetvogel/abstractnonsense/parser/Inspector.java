package nl.jessetvogel.abstractnonsense.parser;

import nl.jessetvogel.abstractnonsense.core.*;
import java.util.StringJoiner;

public class Inspector {

    private final Session session;

    Inspector(Session session) {
        this.session = session;
    }

    public String inspect(Morphism f) {
        return inspect(session.owner(f), f);
    }

    public String inspect(Diagram diagram, Morphism f) {
        String type = (f.k == 0) ? diagram.str(diagram.getSession().cat(f)) : diagram.str(diagram.getSession().dom(f)) + " -> " + diagram.str(diagram.getSession().cod(f));
        return diagram.str(f) + " : " + type;
    }

    public String inspect(Property property) {
        StringBuilder sb = new StringBuilder();
        sb.append("Property ").append(property.name).append("\n");

        // Data
        StringJoiner sj = new StringJoiner(", ", "Data: ", "\n");
        for(Morphism f : property.context.data)
            sj.add(inspect(property.context, f));
        sb.append(sj.toString());

        // Definition
        if(property.definition != null)
            sb.append("Definition: ").append(property.context.str(property.definition)).append("\n");

        sb.append(inspect(property.context));
        return sb.toString();
    }

    public String inspect(Theorem theorem) {
        StringBuilder sb = new StringBuilder();
        sb.append("Theorem ").append(theorem.name).append("\n");

        // Use
        StringJoiner sj = new StringJoiner(", ", "Data: ", "\n");
        for(Morphism f : theorem.data)
            sj.add(inspect(theorem, f));
        sb.append(sj.toString());

        // With
        sj = new StringJoiner(", ", "Conditions: ", "\n");
        sj.setEmptyValue("");
        for(Morphism f : theorem.getConditions())
            sj.add(theorem.str(f));
        sb.append(sj.toString());

        // Then
        sj = new StringJoiner(", ", "Conclusions: ", "\n");
        sj.setEmptyValue("");
        for(Morphism f : theorem.getConclusions())
            sj.add(theorem.str(f));
        sb.append(sj.toString());

        sb.append(inspect((Diagram) theorem));
        return sb.toString();
    }

    public String inspect(Diagram diagram) {
        StringBuilder sb = new StringBuilder();
        for(int index : diagram.indices) {
            if (session.nCat.contains(index)) // Do not display categories of n-categories!
                continue;
            sb.append("[").append(index).append("] ").append(inspect(session.morphismFromIndex(index))).append("\n");
        }
        return sb.toString();
    }
}
