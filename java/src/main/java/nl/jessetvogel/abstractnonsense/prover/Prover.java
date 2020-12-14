package nl.jessetvogel.abstractnonsense.prover;

import nl.jessetvogel.abstractnonsense.core.*;
import nl.jessetvogel.abstractnonsense.core.Representation;

import java.util.*;

public class Prover extends Diagram {

    private final Diagram target;

    private final Map<Morphism, Goal> goals;
    private final Queue<Goal> queue;
    private final List<Implication> implications;

    private List<String> proof;

    public Prover(Session session, Diagram target) {
        super(session, target);
        this.target = target;

        goals = new HashMap<>();
        queue = new LinkedList<>();
        implications = new ArrayList<>();
    }

    public boolean prove(Morphism P, int money) {
        // P must be a Proposition
        if (!session.cat(P).equals(session.Prop))
            return false;

        // Create a new list to contain the proof
        proof = new ArrayList<>();

        // Create goal for P
        Goal finalGoal = createGoal(P, money);
        queue.add(finalGoal);

        // As long as the final goal is not proven, and the queue is non-empty, try to prove goals
        while (!finalGoal.isProven() && !queue.isEmpty()) {
            Goal goal = queue.poll();

            // We do not consider proven or unnecessary goals
            if (goal.isProven()) // || (goal != finalGoal && goal.isUnused())) // TODO: how to detect if a goal is used / unused?
                continue;

            // Find applicable theorems
            considerGoal(goal);
        }

        // Print the proof
        for (String line : proof)
            session.print("\uD83D\uDCA1 " + line);

        return finalGoal.isProven();
    }

    private void considerGoal(Goal goal) {
        session.print("\uD83D\uDCCC Consider the goal " + target.str(goal.P) + " ($" + goal.money + ")");

        // If there is no money left, we can't buy anything!
        if (goal.money == 0)
            return;

        // See if any other propositions imply our goal
        for (Representation rep : target.getRepresentations(session.True)) {
            if (rep.type == Representation.Type.HOM && rep.data.get(1).equals(goal.P)) {
                Morphism Q = rep.data.get(0);
                createImplication(
                        goal,
                        new ArrayList<>(Collections.singletonList(Q)),
                        goal.money - 1,
                        target.str(Q) + " implies " + target.str(goal.P)
                );
            }
        }

        // See if any representation is implied by a theorem
        for (Representation repP : target.getRepresentations(goal.P)) {
            // Consider some special cases
            if (repP.type == Representation.Type.AND)
                considerAnd(goal, repP);
            if (repP.type == Representation.Type.OR)
                considerOr(goal, repP);
            if (repP.type == Representation.Type.HOM)
                considerImplies(goal, repP);

            // Find applicable theorems
            for (Theorem thm : session.getTheorems()) {
                if (goal.isProven())
                    return;
                for (Morphism Q : thm.getConclusions()) {
                    if (goal.isProven())
                        return;

                    // If Q == P already, then the theorem satisfies
                    if (Q.equals(goal.P)) {
                        Mapping mapping = new Mapping(thm, target);
                        considerTheoremPartialMapping(goal, thm, mapping);
                        continue;
                    }

                    // If Q does not belong to the theorem (context), then no (Q should equal P already)
                    if (!thm.owns(Q))
                        continue;

                    // Is there a representation of Q that induces P?
                    List<Representation> repsQ = thm.getRepresentations(Q);
                    for (Representation repQ : repsQ) {
                        Mapping mapping = mappingFromRepresentations(thm, repQ, repP);
                        if (mapping != null)
                            considerTheoremPartialMapping(goal, thm, mapping);
                    }
                }
            }
        }
    }

    private void considerAnd(Goal goal, Representation rep) {
        createImplication(goal, rep.data, goal.money, "Trivial");
    }

    private void considerOr(Goal goal, Representation rep) {
        createImplication(goal, rep.data.subList(0, 1), goal.money - 1, "Trivial");
        createImplication(goal, rep.data.subList(1, 2), goal.money - 1, "Trivial");
    }

    private void considerImplies(Goal goal, Representation rep) {
        // TODO
//        if(rep.property == Global.Implies)
//            implicationFromConditions(goal, rep.data.subList(1, diagram.knowsInstance(rep.data.get(1)) ? 1 : 2), goal.money - 1);
    }

    private void considerTheoremPartialMapping(Goal goal, Theorem thm, Mapping mapping) {
        // Search for possible mappings, and consider them all
        List<Mapping> mappings = new ArrayList<>();
        Searcher searcher = new Searcher(mapping);
        searcher.search(mappings);
        for (Mapping m : mappings)
            considerTheorem(goal, thm, m);
    }

    private void considerTheorem(Goal goal, Theorem thm, Mapping mapping) {
        // Construct message before applying theorem, as otherwise str(P) might evaluate to True otherwise
        String message = "By Theorem " + thm.name + " applied to (" + target.strList(mapping.map(thm.data)) + "), we have " + target.str(goal.P);

        // See if we can apply the theorem using mapping
        int i = proof.size();
        List<Morphism> result = thm.apply(mapping);
        if (result == null)
            return;

        // If the theorem was already applied, insert proof at appropriate place
        if (result.isEmpty())
            proof.add(i, message);

        // Otherwise, create Implication
        else createImplication(goal, result, goal.money - 1, message);
    }

    private void createImplication(Goal goal, List<Morphism> conditions, int money, String message) {
        // It is impossible to prove False, of course
        if (conditions.contains(session.False))
            return;

        // If there are no (non-True) conditions, immediately conclude
        conditions.removeIf(P -> P.equals(session.True));
        if (conditions.isEmpty()) {
            proof.add(message);

            try {
                session.identify(goal.P, session.True);
            } catch (CreationException e) {
                e.printStackTrace();
            }
            return;
        }

        // Find or create goal for each condition
        List<Goal> listGoals = new ArrayList<>();
        for (Morphism P : conditions) {
            Goal g = goals.get(P);
            // Create new goal if it does not yet exist
            if (g == null)
                g = createGoal(P, money);
                // If the goal g was given at least this much money before, it makes no sense to try to prove this goal again!
            else if (g.money >= money)
                return;
            else g.money = money;

            listGoals.add(g);
        }

        // Create Implication, and add goals to the queue
        Implication implication = new Implication(goal, conditions, message);
        implications.add(implication);
        queue.addAll(listGoals);
    }

    private Mapping mappingFromRepresentations(Context context, Representation r, Representation s) {
        if (r.type != s.type)
            return null;
        if (r.property != s.property)
            return null;

        Mapping mapping = new Mapping(context, target);
        int n = r.data.size();
        for (int i = 0; i < n; ++i) {
            Morphism x = r.data.get(i);
            if (context.owns(x) && !mapping.set(x, s.data.get(i)))
                return null;
        }

        return mapping;
    }

    private Goal createGoal(Morphism P, int money) {
        Goal goal = new Goal(P, money);
        goals.put(P, goal);
        return goal;
    }

    private class Goal {

        private Morphism P;
        private int money;

        Goal(Morphism P, int money) {
            this.P = P;
            this.money = money;
        }

        public boolean isProven() {
            return P.equals(session.True);
        }

    }

    private class Implication {

        private final Goal goal;
        private final List<Morphism> conditions;
        private final String message;

        Implication(Goal goal, List<Morphism> conditions, String message) {
            this.goal = goal;
            this.conditions = conditions;
            this.message = message;
        }

        boolean update(Morphism P, Morphism Q, List<MorphismPair> induced) {
            // Replace conditions if necessary
            conditions.replaceAll(R -> (R.index == P.index ? new Morphism(Q.index, R.k) : R));
            // Remove all true conditions
            conditions.removeIf(R -> R.equals(session.True));

            // If this Implication is meaningless, mark as resolved without concluding
            // Also, if this becomes impossible to prove, mark as resolved without concluding
            if (goal.P.equals(session.True) || goal.P.equals(session.False) || conditions.contains(session.False))
                return true;

            // If there are no more conditions, conclude!
            if (conditions.isEmpty()) {
                proof.add(message);
                induced.add(new MorphismPair(goal.P, session.True)); // Note: indeed, all goal.P's were already updated before
                return true;
            }

            return false;
        }
    }

    @Override
    protected void replaceMorphism(Morphism f, Morphism g, List<MorphismPair> induced) throws CreationException {
        super.replaceMorphism(f, g, induced);

        // Don't bother doing anything with Goals and Implications if f and g are not Propositions..
        if (!session.cat(f).equals(session.Prop))
            return;

        // Update goals if necessary
        for (Map.Entry<Morphism, Goal> entry : new HashSet<>(goals.entrySet())) {
            Goal goal = entry.getValue();
            if (goal.P.index == f.index) {
                goals.remove(goal.P);
                goal.P = new Morphism(g.index, goal.P.k);

                // It makes no sense to put True or False back in the map
                if (!g.equals(session.True) && !g.equals(session.False))
                    goals.put(goal.P, goal);
            }
        }

        // Update implications
        implications.removeIf(I -> I.update(f, g, induced));
    }
}
