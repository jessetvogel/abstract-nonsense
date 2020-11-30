package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Mapping {

    private final Session session;
    public final Context context;
    public final Diagram target;
    private final Map<Integer, Integer> mapping; // This is sufficient (and convenient) as we know that k-morphisms will be mapped to k-morphisms!

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

    public boolean set(Morphism x, Morphism y) {
        // If already set, not allowed to clash
        if (maps(x))
            return map(x).equals(y);

        // k-morphisms must map to k-morphisms
        if (x.k != y.k)
            return false;

        // lift as long as x is an identity morphism
        while (session.isIdentity(x)) {
            if (!session.isIdentity(y))
                return false;
            x = new Morphism(x.index, x.k - 1);
            y = new Morphism(y.index, y.k - 1);
        }

        mapping.put(x.index, y.index);

        // Induced mapping for category, domain, codomain (that is, if they need to be mapped)
        if (context.owns(session.cat(x)) ? !set(session.cat(x), session.cat(y)) : (!session.cat(x).equals(session.cat(y))))
            return false;

        if (x.k > 0) {
            if (context.owns(session.dom(x)) ? !set(session.dom(x), session.dom(y)) : !session.dom(x).equals(session.dom(y)))
                return false;
            if (context.owns(session.cod(x)) ? !set(session.cod(x), session.cod(y)) : !session.cod(x).equals(session.cod(y)))
                return false;
        }

        // If x is data, stop here.
        if (context.isData(x))
            return true;
//
//        // TODO: CAN DO MORE HERE ALREADY
//        // TODO: maybe already map everything that depends on what is currently mapped

        return true;
    }

    public boolean maps(Morphism x) {
        return mapping.containsKey(x.index);
    }

    public boolean maps(int index) {
        return mapping.containsKey(index);
    }

    public boolean mapsList(List<Morphism> list) {
        for (Morphism x : list) {
            if (!maps(x))
                return false;
        }
        return true;
    }

    public Morphism map(Morphism x) {
        if (maps(x))
            return new Morphism(mapping.get(x.index), x.k);
        return x;
    }

    public List<Morphism> mapList(List<Morphism> list) {
        List<Morphism> mapped = new ArrayList<>();
        for (Morphism x : list)
            mapped.add(map(x));
        return mapped;
    }

    public boolean isValid() {
        // All context data must be mapped
        for (Morphism x : context.data) {
            if (!maps(x)) {
                System.out.println("Does not map some data");
                return false;
            }
        }

        // Make sure all representations are well-mapped
        Set<Map.Entry<Representation, Morphism>> rToMap = new HashSet<>(context.representations.entrySet());
        boolean updates = true;
        while (updates) {
            updates = false;
            for (Iterator<Map.Entry<Representation, Morphism>> it = rToMap.iterator(); it.hasNext(); ) {
                Map.Entry<Representation, Morphism> entry = it.next();
                // If r is a representation of some datum, nothing to check // TODO: is this even possible? do data have non-symbol represenations?
                if (context.isData(entry.getValue())) {
                    it.remove();
                    continue;
                }

                // Can only extend the mapping to r if all its dependencies are already mapped
                Representation rep = entry.getKey();
                List<Morphism> checklist = new ArrayList<>(rep.data);
                checklist.removeIf(x -> !context.owns(x));
                if (!mapsList(checklist))
                    continue;

                Morphism y;
                try {
                    y = target.createFromRepresentation(rep, this);
                } catch (CreationException e) {
                    System.err.println(e.getMessage());
                    return false;
                }

                if (!set(entry.getValue(), y)) {
                    System.err.println("Failed to map " + context.str(entry.getValue()) + " to " + target.str(y));
                    return false;
                }

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

    public void search(List<Mapping> list) {
        // Consider first datum x which is not yet mapped
        for (Morphism x : context.data) {
            if (maps(x))
                continue;

            // Find candidates y for x
            List<Morphism> candidates = findCandidates(x);

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

    private List<Morphism> findCandidates(Morphism x) {
        Morphism cat = session.cat(x), dom = session.dom(x), cod = session.cod(x);
        boolean filterCat = !context.owns(cat), filterDom = !context.owns(dom), filterCod = !context.owns(cod);

        if (!filterCat && maps(cat)) {
            cat = map(cat);
            filterCat = true;
        }
        if (!filterDom && maps(dom)) {
            dom = map(dom);
            filterDom = true;
        }
        if (!filterCod && maps(cod)) {
            cod = map(cod);
            filterCod = true;
        }

        List<Morphism> candidates = new ArrayList<>();
        for (int j : target.indices) {
            Morphism y = session.morphism(j);
            if(y == null)
                continue;
            if(y.k > x.k)
                continue;
            if(y.k < x.k)
                y = new Morphism(y.index, x.k);

            if(filterCat && !session.cat(y).equals(cat))
                continue;
            if(filterDom && !session.dom(y).equals(dom))
                continue;
            if(filterCod && !session.cod(y).equals(cod))
                continue;

            candidates.add(y);
        }

        return candidates;
    }
}
