package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Mapping {

    protected final Session session;
    public final Context context;
    public final Diagram target;
    protected final Map<Integer, Integer> mapping; // This is sufficient (and convenient) as we know that k-morphisms will be mapped to k-morphisms!

    public Mapping(Context context, Diagram target) {
        this.session = context.session;
        this.context = context;
        this.target = target;
        mapping = new HashMap<>();
    }

    public Mapping(Mapping mapping) {
        this.session = mapping.session;
        this.context = mapping.context;
        this.target = mapping.target;
        this.mapping = new HashMap<>(mapping.mapping);
    }

    public boolean set(Morphism f, Morphism g) {
        // If already determined, not allowed to clash
        if (determined(f))
            return map(f).equals(g);

        // k-morphisms must map to k-morphisms
        if (f.k != g.k)
            return false;

        // Lift as long as f is an identity morphism
        while (session.isIdentity(f)) {
            if (!session.isIdentity(g)) // Identity morphisms must map to identity morphisms
                return false;
            f = new Morphism(f.index, f.k - 1);
            g = new Morphism(g.index, g.k - 1);
        }

        // Put
        put(f.index, g.index);

        // Induced mapping for category, domain, codomain
        if (!set(session.cat(f), session.cat(g)))
            return false;
        if (f.k > 0) {
            if (!set(session.dom(f), session.dom(g)))
                return false;
            if (!set(session.cod(f), session.cod(g)))
                return false;
        }

//        // TODO: CAN DO MORE HERE ALREADY ??
//        // TODO: maybe already map everything that depends on what is currently mapped

        return true;
    }

    protected void put(int i, int j) {
        mapping.put(i, j);
    }

    public boolean determined(Morphism f) {
        return mapping.containsKey(f.index) || !context.owns(f);
    }

    public boolean determined(int index) {
        return mapping.containsKey(index) || !context.owns(index);
    }

    public boolean determinedAll(List<Morphism> list) {
        for (Morphism x : list) {
            if (!determined(x))
                return false;
        }
        return true;
    }

    public Morphism map(Morphism x) {
        if (!context.owns(x))
            return x;
        if (mapping.containsKey(x.index))
            return new Morphism(mapping.get(x.index), x.k);
        return null;
    }

    public List<Morphism> map(List<Morphism> list) {
        List<Morphism> mapped = new ArrayList<>();
        for (Morphism x : list)
            mapped.add(map(x));
        return mapped;
    }

    public boolean valid() {
        // All context data must be mapped
        for (Morphism x : context.data)
            if (!determined(x))
                return false;

        // Make sure all representations are well-mapped
        Set<Map.Entry<Representation, Morphism>> rToMap = new HashSet<>(context.representations.entrySet());
        boolean updates = true;
        while (updates) {
            updates = false;
            for (Iterator<Map.Entry<Representation, Morphism>> it = rToMap.iterator(); it.hasNext(); ) {
                Map.Entry<Representation, Morphism> entry = it.next();
                Representation rep = entry.getKey();
                Morphism f = entry.getValue();

                // Can only extend the mapping to r if all its dependencies are already mapped
                if (!determinedAll(rep.data))
                    continue;

                Morphism g;
                try {
//                    System.out.println("Set g to be the image of " + context.str(f));
                    g = target.morphism(rep.map(this));
                } catch (CreationException e) {
                    System.err.println(e.getMessage());
                    return false;
                }

                if (!set(f, g)) {
//                    System.err.println("Failed to map " + context.str(f) + " to " + target.str(g));
                    return false;
                }

//                System.out.println("Put " + context.str(f) + " [" + f.index + ", " + f.k + "] |-> " + target.str(g) + " [" + g.index + ", " + g.k + "]");

                it.remove();
                updates = true;
            }
        }

        // At this point, everything (should be) well-mapped!
        if (!rToMap.isEmpty()) {
            System.err.println("(Some) representations do not depend on data!");
            return false;
        }

        // Done!
        return true;
    }

}
