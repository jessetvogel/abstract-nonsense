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
        System.out.println("[We consider the goal " + diagram.str(goal.P) + " with money = " + String.valueOf(goal.money) + "]");

        // If we already know a proof, resolve the goal
        if (diagram.knowsInstance(goal.P)) {
            goal.setProven();
            return;
        }

        // If there is no money left, stop
        if(goal.money == 0)
            return;

        // Find applicable theorems
        Book book = (Book) diagram;
        ArrayList<Representation> repsP = diagram.getRepresentations(goal.P);
        for (Theorem thm : book.getTheorems()) {
            if(goal.isProven())
                return;
            for (Morphism x : thm.conclusion.morphisms) {
                if(goal.isProven())
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
                ArrayList<Representation> repsC = thm.getRepresentations(C);
                for (Representation rC : repsC) {
                    for (Representation rP : repsP) {
                        Mapping mapping = mappingFromRepresentations(thm, rC, rP);
                        if(mapping != null)
                            considerTheoremPartialMapping(goal, thm, mapping);
                    }
                }
            }
        }
    }

    private void considerTheoremPartialMapping(Goal goal, Theorem thm, Mapping mapping) {
        // Search for possible mappings, and consider them all
        ArrayList<Mapping> mappings = new ArrayList<>();
        mapping.search(mappings);
        for(Mapping m : mappings)
            considerTheorem(goal, thm, m);
    }

    private void considerTheorem(Goal goal, Theorem thm, Mapping mapping) {
        // Try to apply the theorem using mapping
        ArrayList<Morphism> result = thm.apply(mapping);
        // If it cannot be applied, stop
        if(result == null)
            return;
        // If it was applied (no more conditions), immediately set goal to proven
        if(result.isEmpty()) {
            goal.setProven();
            return;
        }

        // Money for conditions (one less than of goal)
        int money = goal.money - 1; // TODO: maybe change to thm.cost at some point..

        // Create goals for the remaining conditions
        ArrayList<Goal> conditions = new ArrayList<>();
        for(Morphism P : result) {
            Goal g = getGoal(P);
            if(g == null)
                g = createGoal(P, money);
            else if (g.money >= money)
                return;
            else
                g.money = money;
            conditions.add(g);
        }

        // Create Implication, and add goals to the queue
        new Implication(goal, conditions);
        queue.addAll(conditions);
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
        private final ArrayList<Implication> usedFor;

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
        final ArrayList<Goal> conditions;

        public Implication(Goal goal, ArrayList<Goal> conditions) {
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
