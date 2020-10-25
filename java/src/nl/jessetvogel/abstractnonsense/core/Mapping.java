package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Mapping {

    Context context;
    public Diagram target;
    Map<Morphism, Morphism> mapping;

    public Mapping(Context context, Diagram target) {
        this.context = context;
        this.target = target;
        mapping = new HashMap<>();
    }

    public Mapping(Mapping mapping) {
        this.context = mapping.context;
        this.target = mapping.target;
        this.mapping = new HashMap<>(mapping.mapping);
    }

    public boolean set(Morphism x, Morphism y) {
        // If already set, not allowed to clash
        if (maps(x))
            return map(x) == y;

        // Objects must map to objects
        if (x.isObject() && !y.isObject())
            return false;

        // TODO: categories must match!!

        mapping.put(x, y);

        // Induced mapping for category, domain, codomain (that is, if they need to be mapped)
        if (context.owns(x.category) ? !set(x.category, y.category) : (x.category != y.category))
            return false;

        if (!x.isObject()) {
            if (context.owns(x.domain) ? !set(x.domain, y.domain) : x.domain != y.domain)
                return false;
            if (context.owns(x.codomain) ? !set(x.codomain, y.codomain) : x.codomain != y.codomain)
                return false;
        }

        // If x is data, stop here.
        if (context.isData(x))
            return true;

        // TODO: CAN DO MORE HERE ALREADY
        // TODO: maybe already map everything that depends on what is currently mapped

        return true;
    }

    public boolean maps(Morphism x) {
        return mapping.containsKey(x);
    }

    public boolean mapsList(ArrayList<Morphism> list) {
        for (Morphism x : list) {
            if (!maps(x))
                return false;
        }
        return true;
    }

    public Morphism map(Morphism x) {
        if (mapping.containsKey(x))
            return mapping.get(x);
        return x;
    }

    public ArrayList<Morphism> mapList(ArrayList<Morphism> list) {
        ArrayList<Morphism> mapped = new ArrayList<>();
        for (Morphism x : list)
            mapped.add(map(x));
        return mapped;
    }

    public boolean isValid() {
        // All context data must be mapped
        for (Morphism x : context.data) {
            if (!maps(x))
                return false;
        }

        // Make sure all representations are well-mapped
        Set<Map.Entry<Representation, Morphism>> rToMap = new HashSet<>(context.representations.entrySet());
        boolean updates = true;
        while (updates) {
            updates = false;
            for (Iterator<Map.Entry<Representation, Morphism>> it = rToMap.iterator(); it.hasNext(); ) {
                Map.Entry<Representation, Morphism> entry = it.next();
                // If r is a representation of some datum, nothing to check
                if (context.isData(entry.getValue())) {
                    it.remove();
                    continue;
                }

                // Can only extend the mapping to r if all its dependencies are already mapped
                Representation rep = entry.getKey();
                ArrayList<Morphism> checklist = new ArrayList<>(rep.data);
                checklist.removeIf(x -> !context.owns(x));
                if (!mapsList(checklist))
                    continue;

                Morphism y;
                try {
                    y = target.createFromPlaceholders(rep, this);
                } catch (CreationException e) {
                    return false;
                }

                if (!set(entry.getValue(), y))
                    return false;

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

    public void search(ArrayList<Mapping> list) {
        // Consider first datum x which is not yet mapped
        for (Morphism x : context.data) {
            if (maps(x))
                continue;

            // Find candidates y for x
            ArrayList<Morphism> candidates = findCandidates(x);

            if (candidates.isEmpty())
                return;
            if (candidates.size() == 1) {
                if (!set(x, candidates.get(0)))
                    return;
                continue;
            }

            // Branch out
            for (Morphism y : candidates) {
                Mapping m = new Mapping(this);
                if (m.set(x, y))
                    m.search(list);
            }

            return;
        }

        // If all data was mapped, it is only left to validate the mapping
        if (isValid())
            list.add(this);
    }

    private ArrayList<Morphism> findCandidates(Morphism x) {
        Morphism C = map(x.category);
        ArrayList<Morphism> candidates = new ArrayList<>();
        boolean xIsObject = x.isObject();
        for (Morphism y : target.morphisms) {
            if (y.category == C && (!xIsObject || y.isObject()))
                candidates.add(y);
        }

        if (maps(x.domain)) {
            Morphism yDomain = map(x.domain);
            candidates.removeIf(y -> y.domain != yDomain);
        }
        if (maps(x.codomain)) {
            Morphism yCodomain = map(x.codomain);
            candidates.removeIf(y -> y.codomain != yCodomain);
        }

        return candidates;
    }
}
