package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Theorem extends Context {

    private final List<Morphism> conditions;
    private final List<Morphism> conclusions;

    public Theorem(Session session, String name) {
        super(session, session, name);
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

    @Override
    protected void replaceMorphism(Morphism f, Morphism g, List<MorphismPair> induced) throws CreationException {
        super.replaceMorphism(f, g, induced);
        conditions.replaceAll(z -> (z.index == f.index ? new Morphism(g.index, z.k) : z));
        conclusions.replaceAll(z -> (z.index == f.index ? new Morphism(g.index, z.k) : z));
    }

    public List<Morphism> getConditions() {
        return conditions;
    }
}
