package nl.jessetvogel.abstractnonsense.prover;

import nl.jessetvogel.abstractnonsense.core.*;

import java.util.*;

public class Searcher extends Mapping {

    // TODO: maybe it is unnecessary to explicitly keep track of 'depth' just keep a stack and work on the last layer, so to say
    private List<Integer> recentlyMapped;

    public Searcher(Context context, Diagram target) {
        super(context, target);
        recentlyMapped = null;
    }

    public Searcher(Mapping mapping) {
        super(mapping);
        recentlyMapped = null;
    }

    public void search(List<Mapping> mappings) {
        // Setup search plan
        SearchPlan plan = new SearchPlan();

        // Find mappings
        find(plan, mappings, 0);
    }

    private void find(SearchPlan plan, List<Mapping> mappings, int depth) {
        // If the whole queue is mapped, the mapping should be complete, and we store it!
        if(depth >= plan.queue.size()) {
            mappings.add(new Mapping(this));
            return;
        }

        // Get morphism from the queue, if it is already determined, we can skip this step
        Morphism f = plan.queue.get(depth);
        if(determined(f)) {
            find(plan, mappings, depth + 1);
            System.err.println("Weird, but okay..");
            return;
        }

        // TODO: maybe these were already found!
        List<Morphism> candidates = findCandidates(target, f);
        if(candidates.isEmpty())
            return;

        List<Integer> mapped = new ArrayList<>();
        for(Morphism g : candidates) {
            // Set list and try mapping
            recentlyMapped = mapped;
            if(set(f, g) && setInduced(plan.induced.get(depth)))
                find(plan, mappings, depth + 1);

            // Unset and clear list
            unset(mapped);
            mapped.clear();
            recentlyMapped = null;
        }
    }

    @Override
    protected void put(int i, int j) {
        mapping.put(i, j);

        // Remember what index was put, so it can be undone later
        if(recentlyMapped != null)
            recentlyMapped.add(i);
    }

    private boolean setInduced(List<Representation> reps) {
        for(Representation rep : reps) {
            // TODO: this should not be necessary!
            if (!determinedAll(rep.data)) {
                System.err.println("The whole idea was that this works..");
                continue;
            }

            Morphism f = context.representations.get(rep);
            Morphism g;
            try {
                g = target.morphism(rep.map(this));
            } catch (CreationException e) {
                System.err.println(e.getMessage());
                continue;
            }

            if (!set(f, g))
                return false;
        }
        return true;
    }

    private void unset(List<Integer> indices) {
        // Remove all mappings from the last layer, and delete the layer
        for(int index : indices)
            mapping.remove(index);
    }

    private List<Morphism> findCandidates(Diagram diagram, Morphism f) {
        List<Morphism> list = new ArrayList<>();

        // These should be well-mapped at this point
        Morphism cat = map(session.cat(f));
        Morphism dom = map(session.dom(f));
        Morphism cod = map(session.cod(f));

        // Special case of Prop, there are only two options
        if(cat.equals(session.Prop)) {
            list.add(session.True);
            list.add(session.False);
            return list;
        }

        // Find morphisms in target whose cat, dom, cod are what we are looking for
        for(int index : diagram.indices) {
            Morphism g = session.morphismFromIndex(index, f.k);
            if(g == null || !session.cat(g).equals(cat) || !session.dom(g).equals(dom) || !session.cod(g).equals(cod))
                continue;

            list.add(g);
        }

        // Find candidates in parent?
        if(diagram.hasParent() && !diagram.owns(cat) && !diagram.owns(dom) && !diagram.owns(cod))
            list.addAll(findCandidates(diagram.getParent(), f));

        return list;
    }

    private class SearchPlan {

        // TODO: independence blocks, so that you can remember candidates! ?

        List<Morphism> queue;
        List<List<Representation>> induced;

        SearchPlan() {
            // Set queue and lists of induced mappings
            setQueue();
            setInduced();

            // Print the plan
//            System.out.println("Search queue: " + context.strList(queue));
//            int i = 0;
//            for(List<Representation> list : induced) {
//                // TODO: printing not very nice, because all assumptions are already mapped to True..
//                System.out.println("(" + (i++) + ") induces " + context.strList(list.stream().map(context.representations::get).collect(Collectors.toList())));
//            }
        }

        private void setQueue() {
            queue = new ArrayList<>();
            for(Morphism f : context.data)
                addToQueue(f);
        }

        private void addToQueue(Morphism f) {
            // TODO: what if f is an identity morphism?
            if(queue.contains(f))
                return;
            if(!determined(f)) {
                addToQueue(session.cat(f));
                if(f.k > 0) {
                    addToQueue(session.dom(f));
                    addToQueue(session.cod(f));
                }
                queue.add(f);
            }
        }

        private void setInduced() {
            induced = new ArrayList<>();
            Set<Integer> marked = new HashSet<>();

            Set<Map.Entry<Representation, Morphism>> repsToInduce = new HashSet<>(context.representations.entrySet());

            for(int depth = 0; depth < queue.size(); ++depth) {
                // Add morphism (index) from queue to marked
                marked.add(queue.get(depth).index);
                List<Representation> list = new ArrayList<>();
                boolean updates = true;
                while(updates) {
                    updates = false;
                    for (Iterator<Map.Entry<Representation, Morphism>> it = repsToInduce.iterator(); it.hasNext(); ) {
                        Map.Entry<Representation, Morphism> entry = it.next();
                        Representation rep = entry.getKey();
                        Morphism f = entry.getValue();

                        // Can only be induced if all its data is marked
                        if(!determinedOrMarked(marked, rep.data))
                            continue;

                        list.add(rep);
                        marked.add(f.index);
                        it.remove();
                        updates = true;
                    }
                }

                induced.add(list);
            }
        }

        private boolean determinedOrMarked(Set<Integer> marked, List<Morphism> morphisms) {
            for(Morphism f : morphisms) {
                if (!determined(f) && !marked.contains(f.index))
                    return false;
            }
            return true;
        }

    }

}
