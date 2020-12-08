package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Theorem extends Context {

    public final String name;
    private final List<Morphism> conditions;
    private final List<Morphism> conclusions;

    public Theorem(Session session, String name) {
        super(session, session);
        this.name = name;
        conclusions = new ArrayList<>();
        conditions = new ArrayList<>();
    }

    public void addCondition(Morphism P) {
        conditions.add(P);
    }

    public void addConclusion(Morphism Q) {
        conclusions.add(Q);
    }

    public List<Morphism>  apply(Mapping mapping) {
        // Mapping must be valid
        if (!mapping.valid())
            return null;

        // See what conditions are already satisfied, and which are not
        List<Morphism> list = new ArrayList<>();
        for (Morphism P : conditions) {
            Morphism Q = mapping.map(P);
            if (!Q.equals(session.True))
                list.add(Q);
        }

        // Only if all conditions are satisfied, we apply the conclusion
        if (list.isEmpty())
            applyConclusion(mapping);

        return list;
    }

    private void applyConclusion(Mapping mapping) {
        for (Morphism Q : conclusions) {
            try {
                session.identify(mapping.map(Q), session.True);
            } catch (Exception e) {
                System.err.println("Failed! " + e.getMessage());
            }
        }
    }

    public List<Morphism> getConclusions() {
        return conclusions;
    }

//    private boolean applyConclusion(Mapping mapping) {
//        // Extend mapping to all morphisms of the conclusion
//        Set<Map.Entry<Representation, Morphism>> rToMap = new HashSet<>(conclusion.representations.entrySet());
//        rToMap.removeIf(entry -> mapping.maps(entry.getValue()));
//        while (!rToMap.isEmpty()) {
//            for (Iterator<Map.Entry<Representation, Morphism>> it = rToMap.iterator(); it.hasNext(); ) {
//                Map.Entry<Representation, Morphism> entry = it.next();
//                List<Morphism> checklist = new ArrayList<>(entry.getKey().data); // These morphisms must be mapped before we can map the value of this representation
//                checklist.removeIf(x -> !conclusion.owns(x)); // TODO: 'that the conclusion does not own'? Or to be safe just !this.owns(x)
//                if (!mapping.mapsList(checklist))
//                    continue;
//
//                Morphism y;
//                try {
//                    y = mapping.target.createFromRepresentation(entry.getKey(), mapping);
//                } catch (CreationException e) {
//                    e.printStackTrace();
//                    return false;
//                }
//
//                if (!mapping.set(entry.getValue(), y))
//                    return false;
//
//                it.remove();
//            }
//        }
//
//        // Finally, create objects in other_diagram for all objects that do not have a representation in the conclusion (i.e. mostly proofs of statements)
//        for (int i : conclusion.indices) {
//            if (!mapping.maps(i)) {
//                Morphism x = session.morphism(i);
//
//                Morphism y;
//                try {
//                    y = session.createMorphism(mapping.target, mapping.map(session.cat(x)), x.k, mapping.map(session.dom(x)), mapping.map(session.cod(x)));
//                }
//                catch(CreationException e) {
//                    System.err.println("Why would this fail???");
//                    return false;
//                }
//
//                if (!mapping.set(x, y))
//                    return false;
//            }
//        }
//
//        System.out.println("Apply Theorem " + name + " to (" + mapping.target.strList(mapping.mapList(data)) + ") to conclude");
//        return true;
//    }

    @Override
    protected void replaceMorphism(Morphism f, Morphism g, List<MorphismPair> induced) throws CreationException {
        super.replaceMorphism(f, g, induced);
        conditions.replaceAll(z -> (z.index == f.index ? new Morphism(g.index, z.k) : z));
        conclusions.replaceAll(z -> (z.index == f.index ? new Morphism(g.index, z.k) : z));
    }
}
