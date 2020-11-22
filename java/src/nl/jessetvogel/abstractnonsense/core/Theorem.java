package nl.jessetvogel.abstractnonsense.core;

import java.lang.reflect.Array;
import java.util.*;

public class Theorem extends Context {

    public final String name;
    public final Diagram conclusion;
    private final List<Morphism> conditions;

    public Theorem(Diagram parent, String name) {
        super(parent);
        this.name = name;
        conditions = new ArrayList<>();
        conclusion = new Diagram(this);
    }

    public void addCondition(Morphism x) {
        conditions.add(x);
    }

    public List<Morphism> apply(Mapping mapping) {
        // Mapping must be valid
        if(!mapping.isValid())
            return null;

        // See what conditions are already satisfied, and which are not
        List<Morphism> list = new ArrayList<>();
        for(Morphism P : conditions) {
            Morphism Q = mapping.map(P);
            if (!mapping.target.knowsInstance(Q))
                list.add(Q);
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
                List<Morphism> checklist = new ArrayList<>(entry.getKey().data);
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
        List<Morphism> __tmp__ = new ArrayList<>();
        for (Morphism x : conclusion.morphisms) {
            if (!mapping.maps(x)) {
                Morphism C = mapping.map(x.category);
                __tmp__.add(C);
                Morphism y;
                if (x.isObject())
                    y = mapping.target.createObject(C);
                else
                    y = mapping.target.createMorphism(mapping.map(x.domain), mapping.map(x.codomain), x.covariant);

                if (!mapping.set(x, y))
                    return false;
            }
        }

        System.out.println("Apply Theorem " + name + " to (" + mapping.target.strList(mapping.mapList(data)) + ") to conclude " + mapping.target.strList(__tmp__));
        return true;
    }

    protected void replaceMorphism(Morphism x, Morphism y, List<InducedEquality> induced) throws CreationException {
        super.replaceMorphism(x, y, induced);
        if (conditions.contains(x))
            conditions.replaceAll(z -> (z == x ? y : z));
    }
}
