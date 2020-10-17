package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Theorem extends Context {

    String name;
    Diagram conclusion;

    public Theorem(Diagram parent, String name) {
        super(parent);
        this.name = name;
        conclusion = new Diagram(this);
    }

    public Diagram getConclusion() {
        return conclusion;
    }

    public boolean tryApplication(Mapping mapping) {
        // Search for mapping
        if(!mapping.search())
            return false;

        // Apply conclusion
        Set<Map.Entry<Representation, Morphism>> rToMap = new HashSet<>(conclusion.representations.entrySet());
        rToMap.removeIf(entry -> mapping.maps(entry.getValue()));
        while(!rToMap.isEmpty()) {
            for(Iterator<Map.Entry<Representation, Morphism>> it = rToMap.iterator(); it.hasNext(); ) {
                Map.Entry<Representation, Morphism> entry = it.next();
                ArrayList<Morphism> checklist = new ArrayList<>(entry.getKey().data);
                checklist.removeIf(x -> !conclusion.owns(x));
                if(!mapping.mapsList(checklist))
                    continue;

                Morphism y;
                try {
                    y = mapping.target.createFromPlaceholders(entry.getKey(), mapping);
                } catch (CreationException e) {
                    e.printStackTrace();
                    return false;
                }

                if(!mapping.set(entry.getValue(), y))
                    return false;

                it.remove();
            }
        }

        // Finally, create objects in other_diagram for all objects that do not have a representation in the conclusion (i.e. mostly proofs of statements)
        for(Morphism x : conclusion.morphisms) {
            if(!mapping.maps(x)) {
                Morphism C = mapping.map(x.category);
                Morphism y;
                if(x.isObject())
                    y = mapping.target.createObject(C);
                else
                    y = mapping.target.createMorphism(mapping.map(x.domain), mapping.map(x.codomain), x.covariant);

                if(!mapping.set(x, y))
                    return false;
            }
        }

        System.out.println("Applied Theorem " + name + " to (" + mapping.target.strList(mapping.mapList(data)) + ")");
        return true;
    }
}
