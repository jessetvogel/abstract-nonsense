package nl.jessetvogel.abstractnonsense.prover;

import nl.jessetvogel.abstractnonsense.core.*;

import java.util.*;
import java.util.concurrent.LinkedTransferQueue;

public class Prover {

    Diagram diagram;
    Map<Morphism, Goal> goals;
    Queue<Goal> queue;

    public Prover(Diagram diagram) {
        this.diagram = diagram;
        goals = new HashMap<>();
        queue = new LinkedTransferQueue<>();
    }

    public boolean prove(Morphism P, int money) {
        // P must be a category
        if (!P.isCategory())
            return false;

        // Create goal for P
        Goal finalGoal = createGoal(P, money);
        queue.add(finalGoal);

        // As long as the final goal is not proven, and the queue is non-empty, try to prove goals
        while (!finalGoal.isProven() && !queue.isEmpty()) {
            Goal goal = queue.poll();

            // We do not consider proven or unnecessary goals
            if (goal.isProven() || (goal != finalGoal && goal.isUnused()))
                continue;

            // Find applicable theorems
            considerGoal(goal);
        }

        return finalGoal.isProven();
    }

    public void considerGoal(Goal goal) {
        System.out.println("[We consider the goal " + diagram.str(goal.P) + " with money = " + goal.money + "]");

        // If we already know a proof, resolve the goal
        if (diagram.knowsInstance(goal.P)) {
            goal.setProven();
            return;
        }

        // If there is no money left, stop
        if(goal.money == 0)
            return;

        // Get representations
        for(Representation repP : diagram.getRepresentations(goal.P)) {
            // Consider some special cases
            considerAnd(goal, repP);
            considerOr(goal, repP);
            considerImplies(goal, repP);

            // Find applicable theorems
            Book book = (Book) diagram;

            for (Theorem thm : book.getTheorems()) {
                if (goal.isProven())
                    return;
                for (Morphism x : thm.conclusion.morphisms) {
                    if (goal.isProven())
                        return;

                    // x MUST BE AN OBJECT
                    if (!x.isObject())
                        continue;

                    // QUESTION: IS THERE ANY WAY THAT x.category WILL EQUAL P WHEN APPLYING THE THEOREM ??
                    Morphism C = x.category;

                    // IF x.category == P ALREADY, THEN YES (with no conditions)
                    if (C == goal.P) {
                        Mapping mapping = new Mapping(thm, diagram);
                        considerTheoremPartialMapping(goal, thm, mapping);
                        continue;
                    }

                    // IF x.category IS OWNED BY THE CONCLUSION (I.E. IT WILL DEPEND ON SOMETHING 'THAT EXISTS'), NO MAPPING WILL BE POSSIBLE
                    if (thm.conclusion.owns(C))
                        continue;

                    // IF x.category DOES NOT BELONG TO THE THEOREM (CONTEXT), THEN NO (otherwise x.category should equal P already)
                    if (!thm.owns(C))
                        continue;

                    // WELL, IF x.category IS DATA, THEN YES (with condition x.category -> P)
                    if (thm.isData(C)) {
                        Mapping mapping = new Mapping(thm, diagram);
                        if (mapping.set(C, goal.P))
                            considerTheoremPartialMapping(goal, thm, mapping);
                        continue;
                    }

                    // AT THIS POINT, IF AND ONLY IF SOME REPRESENTATION OF x.category INDUCES P
                    List<Representation> repsC = thm.getRepresentations(C);
                    for (Representation repC : repsC) {
                        Mapping mapping = mappingFromRepresentations(thm, repC, repP);
                        if (mapping != null)
                            considerTheoremPartialMapping(goal, thm, mapping);
                    }
                }
            }
        }
    }

    private void considerAnd(Goal goal, Representation rep) {
        if(rep.property == Global.And)
            implicationFromConditions(goal, rep.data, goal.money); // TODO: I think this should be fine
    }

    private void considerOr(Goal goal, Representation rep) {
        if(rep.property == Global.Or) {
            implicationFromConditions(goal, rep.data.subList(0, diagram.knowsInstance(rep.data.get(0)) ? 0 : 1), goal.money - 1);
            implicationFromConditions(goal, rep.data.subList(1, diagram.knowsInstance(rep.data.get(1)) ? 1 : 2), goal.money - 1);
        }
    }

    private void considerImplies(Goal goal, Representation rep) {
        if(rep.property == Global.Implies)
            implicationFromConditions(goal, rep.data.subList(1, diagram.knowsInstance(rep.data.get(1)) ? 1 : 2), goal.money - 1);
    }

    private void implicationFromConditions(Goal goal, List<Morphism> conditions, int money) {
        // If there are no conditions, the goal is immediately proven
        if(conditions.isEmpty()) {
            goal.setProven();
            return;
        }

        // Find or create goal for each condition
        List<Goal> goals = new ArrayList<>();
        for(Morphism P : conditions) {
            Goal g = getGoal(P);
            if(g == null)
                g = createGoal(P, money);
            else if (g.money >= money)
                return;
            else
                g.money = money;
            goals.add(g);
        }

        // Create Implication, and add goals to the queue
        new Implication(goal, goals);
        queue.addAll(goals);
    }

    private void considerTheoremPartialMapping(Goal goal, Theorem thm, Mapping mapping) {
        // Search for possible mappings, and consider them all
        List<Mapping> mappings = new ArrayList<>();
        mapping.search(mappings);
        for(Mapping m : mappings)
            considerTheorem(goal, thm, m);
    }

    private void considerTheorem(Goal goal, Theorem thm, Mapping mapping) {
        // Try to apply the theorem using mapping
        List<Morphism> result = thm.apply(mapping);
        // If it cannot be applied, stop
        if(result != null)
            implicationFromConditions(goal, result, goal.money - 1);
    }

    private Mapping mappingFromRepresentations(Context context, Representation r, Representation s) {
        if (r.type != s.type)
            return null;
        if (r.type == Representation.Type.PROPERTY_APPLICATION && r.property != s.property)
            return null;

        Mapping mapping = new Mapping(context, diagram);
        int n = r.data.size();
        for (int i = 0; i < n; ++i) {
            Morphism x = r.data.get(i);
            if (context.owns(x) && !mapping.set(x, s.data.get(i)))
                return null;
        }

        return mapping;
    }

    private Goal getGoal(Morphism P) {
        return goals.get(P);
    }

    private Goal createGoal(Morphism P, int money) {
        Goal goal = new Goal(P, money);
        goals.put(P, goal);
        return goal;
    }

    private static class Goal {

        public final Morphism P;
        private int money;
        private boolean proven;
        private final List<Implication> usedFor;

        Goal(Morphism P, int money) {
            this.P = P;
            this.money = money;
            proven = false;
            usedFor = new ArrayList<>();
        }

        public boolean isUnused() {
            return usedFor.isEmpty();
        }

        public boolean isProven() {
            return proven;
        }

        public void setProven() {
            if (proven) // Prevents recursion
                return;
            proven = true;
            for (Implication I : usedFor)
                I.removeCondition(this);
            usedFor.clear();
        }
    }

    private class Implication {

        final Goal goal;
        final List<Goal> conditions;

        public Implication(Goal goal, List<Goal> conditions) {
            this.goal = goal;
            this.conditions = conditions;
            for(Goal g : conditions)
                g.usedFor.add(this);
        }

        public void removeCondition(Goal g) {
            conditions.remove(g);
            if (conditions.isEmpty()) {
                goal.setProven();
                diagram.createObject(goal.P); // TODO: where to do this?
            }
        }
    }
}
