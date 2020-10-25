package nl.jessetvogel.abstractnonsense.core;

import java.lang.reflect.Array;
import java.util.*;

public class Theorem extends Context {

    public String name;
    Diagram conclusion;
    ArrayList<Morphism> conditions;

    public Theorem(Diagram parent, String name) {
        super(parent);
        this.name = name;
        conditions = new ArrayList<>();
        conclusion = new Diagram(this);
    }

    public void addCondition(Morphism x) {
        conditions.add(x);
    }

    public Diagram getConclusion() {
        return conclusion;
    }

    public ArrayList<Morphism> tryApplication(Mapping mapping) {
        // Search for mapping
        if(!mapping.search())
            return null;

        // See what conditions are already satisfied, and which are not
        ArrayList<Morphism> list = new ArrayList<>();
        for(Morphism C : conditions) {
            Morphism CMapped = mapping.map(C);
            if (!mapping.target.knowsInstance(CMapped))
                list.add(CMapped);
        }

        // If all conditions are satisfied, we apply the conclusion
        if(list.isEmpty())
            applyConclusion(mapping);

        return list;
    }

    private boolean applyConclusion(Mapping mapping) {
        // Extend mapping to all morphisms of the conclusion
        Set<Map.Entry<Representation, Morphism>> rToMap = new HashSet<>(conclusion.representations.entrySet());
        rToMap.removeIf(entry -> mapping.maps(entry.getValue()));
        while (!rToMap.isEmpty()) {
            for (Iterator<Map.Entry<Representation, Morphism>> it = rToMap.iterator(); it.hasNext(); ) {
                Map.Entry<Representation, Morphism> entry = it.next();
                ArrayList<Morphism> checklist = new ArrayList<>(entry.getKey().data);
                checklist.removeIf(x -> !conclusion.owns(x));
                if (!mapping.mapsList(checklist))
                    continue;

                Morphism y;
                try {
                    y = mapping.target.createFromPlaceholders(entry.getKey(), mapping);
                } catch (CreationException e) {
                    e.printStackTrace();
                    return false;
                }

                if (!mapping.set(entry.getValue(), y))
                    return false;

                it.remove();
            }
        }

        // Finally, create objects in other_diagram for all objects that do not have a representation in the conclusion (i.e. mostly proofs of statements)
        ArrayList<Morphism> __ = new ArrayList<>();
        for (Morphism x : conclusion.morphisms) {
            if (!mapping.maps(x)) {
                Morphism C = mapping.map(x.category);
                __.add(C);
                Morphism y;
                if (x.isObject())
                    y = mapping.target.createObject(C);
                else
                    y = mapping.target.createMorphism(mapping.map(x.domain), mapping.map(x.codomain), x.covariant);

                if (!mapping.set(x, y))
                    return false;
            }
        }

        System.out.println("Apply Theorem " + name + " to (" + mapping.target.strList(mapping.mapList(data)) + ") to conclude " + mapping.target.strList(__));
        return true;
    }
}
