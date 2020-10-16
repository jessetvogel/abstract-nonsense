package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Mapping {

    Context context;
    Diagram target;
    Map<Morphism, Morphism> mapping;

    Mapping(Context context, Diagram target) {
        this.context = context;
        this.target = target;
        mapping = new HashMap<>();
    }

    public boolean set(Morphism x, Morphism y) {
        // Cannot clash
        if(maps(x))
            return map(x) == y;

        // Objects must map to objects
        if(x.isObject() && !y.isObject())
            return false;

        mapping.put(x, y);

        // Induced mapping for category, domain, codomain (that is, if they need to be mapped)
        if(context.owns(x.category) && !set(x.category, y.category))
            return false;

        if(!x.isObject()) {
            if (context.owns(x.domain) && !set(x.domain, y.domain))
                return false;
            if (context.owns(x.codomain) && !set(x.codomain, y.codomain))
                return false;
        }

        // If x is data, stop here. TODO: maybe already map everything that depends on what is currently mapped
        if(context.isData(x))
            return true;

        // TODO: CAN DO MORE HERE ALREADY
        return true;
    }

    public boolean maps(Morphism x) {
        return mapping.containsKey(x);
    }

    public boolean mapsList(ArrayList<Morphism> list) {
        for(Morphism x : list) {
            if(!maps(x))
                return false;
        }
        return true;
    }

    public Morphism map(Morphism x) {
        if(mapping.containsKey(x))
            return mapping.get(x);
        return x;
    }

    public ArrayList<Morphism> mapList(ArrayList<Morphism> list) {
        ArrayList<Morphism> mapped = new ArrayList<>();
        for(Morphism x : list)
            mapped.add(map(x));
        return mapped;
    }


    public boolean validate() {
        // All context data must be mapped
        for(Morphism x : context.data) {
            if(!maps(x)) {
                System.out.println('A');
                return false;
            }
        }

        // Check if all representations are well-mapped (possibly extend if not yet done)
        Set<Map.Entry<Representation, Morphism>> rToMap = context.representations.entrySet();
        boolean updates = true;
        while(updates) {
            updates = false;
            for(Iterator<Map.Entry<Representation, Morphism>> it = rToMap.iterator(); it.hasNext(); ) {
                Map.Entry<Representation, Morphism> entry = it.next();
                // If r is a representation of some datum, nothing to check
                if(context.isData(entry.getValue())) {
                    it.remove();
                    continue;
                }

                // Can only extend the mapping to r if all its dependencies are already mapped
                Representation rep = entry.getKey();
                ArrayList<Morphism> checklist = new ArrayList<>(rep.data);
                checklist.removeIf(x -> !context.owns(x));
                if(!mapsList(checklist))
                    continue;

                Morphism y;
                try {
                    y = target.createFromPlaceholders(rep, this);
                } catch (CreationException e) {
                    System.out.println('B');
                    return false;
                }

                if(!set(entry.getValue(), y)) {
                    System.out.println('C');
                    return false;
                }

                it.remove();
                updates = true;
            }
        }

        // At this point, everything is well-mapped!

        // Now, verify the conditions
        for(Morphism C : context.conditions) {
            Morphism CMapped = map(C);
            if (!target.knowsInstance(CMapped)) {
                System.out.println("Cannot verify condition: " + target.str(CMapped));
                return false;
            }
        }

        // Done!
        return true;
    }
}
