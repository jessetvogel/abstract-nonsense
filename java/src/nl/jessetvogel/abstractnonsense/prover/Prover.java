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

    public boolean prove(Morphism P) {
        // P must be a category
        if (!P.isCategory())
            return false;

        // Create goal for P
        Goal finalGoal = goalFromProp(P);
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
//        System.out.println("We consider the goal " + diagram.str(goal.P));

        // If we already know a proof, resolve the goal
        if (diagram.knowsInstance(goal.P)) {
            goal.setProven();
            return;
        }

        // Find applicable theorems
        Book book = (Book) diagram;
        ArrayList<Representation> repsP = diagram.getRepresentations(goal.P);
        for (Theorem thm : book.getTheorems()) {
            Diagram conclusion = thm.getConclusion();
            for (Morphism x : conclusion.morphisms) {
                // x MUST BE AN OBJECT
                if (!x.isObject())
                    continue;

                //QUESTION: IS THERE ANY WAY THAT x.category WILL EQUAL P WHEN APPLYING THE THEOREM ??
                Morphism C = x.category;

                // IF x.category == P ALREADY, THEN YES (with no conditions)
                if (C == goal.P) {
                    Mapping mapping = new Mapping(thm, diagram);
                    if(considerTheoremApplication(goal, thm, mapping))
                        return;
                    continue;
                }

                // IF x.category IS OWNED BY THE CONCLUSION (I.E. IT WILL DEPEND ON SOMETHING 'THAT EXISTS'), NO MAPPING WILL BE POSSIBLE
                if (conclusion.owns(C))
                    continue;

                // IF x.category DOES NOT BELONG TO THE THEOREM (CONTEXT), THEN NO (otherwise x.category should equal P already)
                if (!thm.owns(C))
                    continue;

                // WELL, IF x.category IS DATA, THEN YES (with condition x.category -> P)
                if (thm.isData(C)) {
                    Mapping mapping = new Mapping(thm, diagram);
                    if (!mapping.set(C, goal.P))
                        continue;
                    if(considerTheoremApplication(goal, thm, mapping))
                        return;
                    continue;
                }

                // AT THIS POINT, IF AND ONLY IF SOME REPRESENTATION OF x.category INDUCES P
                ArrayList<Representation> repsC = thm.getRepresentations(C);
                for (Representation rC : repsC) {
                    for (Representation rP : repsP) {
                        Mapping mapping = mappingFromRepresentations(thm, rC, rP);
                        if(mapping != null && considerTheoremApplication(goal, thm, mapping))
                            return;
                    }
                }
            }
        }
    }

    private boolean considerTheoremApplication(Goal goal, Theorem thm, Mapping mapping) {
        // Try to apply the theorem using mapping
        ArrayList<Morphism> result = thm.tryApplication(mapping);
        // If it cannot be applied, stop
        if(result == null)
            return false;
        // If it was applied (no more conditions), immediately set goal to proven
        if(result.isEmpty()) {
            goal.setProven();
            return true;
        }
        // Create an Implication with the goal and the remaining conditions
        ArrayList<Goal> conditions = new ArrayList<>();
        for(Morphism P : result) {
            Goal g = goalFromProp(P);
            conditions.add(g);
            queue.add(g);
        }
        new Implication(goal, conditions);
        return false;
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

    private Goal goalFromProp(Morphism P) {
        Goal goal = goals.get(P);
        if (goal == null) {
            goal = new Goal(P);
            goals.put(P, goal);
        }
        return goal;
    }

    private static class Goal {

        enum Status {
            UNDECIDED,
            SEARCHED,
            PROVEN,
            FAILED
        }

        public final Morphism P;
        public Status status;
        private final ArrayList<Implication> usedFor;

        Goal(Morphism P) {
            this.P = P;
            status = Status.UNDECIDED;
            usedFor = new ArrayList<>();
        }

        public boolean isUnused() {
            return usedFor.isEmpty();
        }

        public boolean isProven() {
            return status == Status.PROVEN;
        }

        public void setProven() {
            if (status == Status.PROVEN) // Prevents recursion
                return;
            status = Status.PROVEN;
            for (Implication I : usedFor)
                I.removeCondition(this);
            usedFor.clear();
        }
    }

    private static class Implication {

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
            if (conditions.isEmpty())
                goal.setProven();
        }

    }
}
