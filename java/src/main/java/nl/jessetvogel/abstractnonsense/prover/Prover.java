package nl.jessetvogel.abstractnonsense.prover;

import nl.jessetvogel.abstractnonsense.core.*;
import nl.jessetvogel.abstractnonsense.core.Representation;

import java.util.*;

public class Prover extends Diagram {

    private final Diagram target;

    private final List<String> proof;
    private final Map<Morphism, Goal> goals;
    private final Queue<Goal> queue;
    private final List<Implication> implications;

    public Prover(Session session, Diagram target) {
        super(session, target, "prover");
        this.target = target;

        proof = new ArrayList<>();
        goals = new HashMap<>();
        queue = new LinkedList<>();
        implications = new ArrayList<>();
    }

    public boolean prove(Morphism P, int money) {
        // P must be a Proposition
        if (!session.cat(P).equals(session.Prop))
            return false;

        // Prepare for a new proof
        proof.clear();

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

        return finalGoal.isProven();
    }

    public List<String> getProof() {
        return new ArrayList<>(proof);
    }

    private boolean considerGoal(Goal goal) {
//        System.out.println("\uD83D\uDCCC Consider the goal " + target.str(goal.P) + " ($" + goal.money + ")");

        // If there is no money left, we can't buy anything!
        if (goal.money == 0)
            return false;

        // See if any other propositions imply our goal
        for (Representation rep : target.getRepresentations(session.True)) {
            if (rep.type == Representation.Type.HOM && rep.data.get(1).equals(goal.P)) {
                Morphism Q = rep.data.get(0);
                if (createImplication(
                        goal,
                        new ArrayList<>(Collections.singletonList(Q)),
                        goal.money - 1,
                        session.str(Q) + " implies " + session.str(goal.P)
                ))
                    return true;
            }
        }

        // See if any representation is implied by a theorem
        for (Representation repP : target.getRepresentations(goal.P)) {
            // Consider some special cases
            if (repP.type == Representation.Type.AND && considerAnd(goal, repP))
                return true;
            if (repP.type == Representation.Type.OR && considerOr(goal, repP))
                return true;
            if (repP.type == Representation.Type.HOM && considerImplies(goal, repP))
                return true;

            // Find applicable theorems
            for (Theorem thm : session.getTheorems()) {
                for (Morphism Q : thm.getConclusions()) {
                    // If Q == P already, then the theorem satisfies
                    if (Q.equals(goal.P)) {
                        Mapping mapping = new Mapping(thm, target);
                        if (considerTheoremPartialMapping(goal, thm, mapping))
                            return true;
                        continue;
                    }

                    // If Q does not belong to the theorem (context), then no (Q should equal P already)
                    if (!thm.owns(Q))
                        continue;

                    // Is there a representation of Q that induces P?
                    List<Representation> repsQ = thm.getRepresentations(Q);
                    for (Representation repQ : repsQ) {
                        Mapping mapping = mappingFromRepresentations(thm, repQ, repP);
                        if (mapping != null && considerTheoremPartialMapping(goal, thm, mapping))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean considerAnd(Goal goal, Representation rep) {
        return createImplication(goal, rep.data, goal.money, "Trivial");
    }

    private boolean considerOr(Goal goal, Representation rep) {
        return createImplication(goal, rep.data.subList(0, 1), goal.money - 1, "Trivial")
                || createImplication(goal, rep.data.subList(1, 2), goal.money - 1, "Trivial");
    }

    private boolean considerImplies(Goal goal, Representation rep) {
        // TODO: the general case

        // If the goal is a negation, search through theorems if it is the negation of some condition
        if (rep.data.get(1).equals(session.False)) {
            Morphism negP = rep.data.get(0);
            for (Representation repNegP : target.getRepresentations(negP)) {
                for (Theorem thm : session.getTheorems()) {
                    for (Morphism Q : thm.getConditions()) {
                        List<Representation> repsQ = thm.getRepresentations(Q);
                        for (Representation repQ : repsQ) {
                            Mapping mapping = mappingFromRepresentations(thm, repQ, repNegP);
                            if (mapping == null)
                                continue;

                            // Search for possible mappings, and consider them all
                            List<Mapping> mappings = new ArrayList<>();
                            Searcher searcher = new Searcher(mapping);
                            searcher.search(mappings);
                            for (Mapping m : mappings) {
                                if (!m.valid()) // TODO: this should not happen, but apparantly something is wrong with the Searcher?
                                    continue;

                                // For each conclusion R of thm, we have implications:
                                // ~R & (thm.conditions - Q) => ~Q (which is P)
                                for (Morphism R : thm.getConclusions()) {
                                    try {
                                        List<Morphism> conditions = new ArrayList<>(thm.getConditions());
                                        conditions.remove(Q);
                                        conditions = m.map(conditions);
                                        conditions.add(thm.morphism(Representation.hom(m.map(R), session.False)));
                                        if (createImplication(goal, conditions, goal.money - 1, "By the negation of Theorem " + thm.name + " applied to (" + session.str(m.map(thm.data)) + "), we have " + session.str(goal.P)))
                                            return true;
                                    } catch (CreationException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean considerTheoremPartialMapping(Goal goal, Theorem thm, Mapping mapping) {
        // Search for possible mappings, and consider them all
        List<Mapping> mappings = new ArrayList<>();
        Searcher searcher = new Searcher(mapping);
        searcher.search(mappings);
        for (Mapping m : mappings)
            if (considerTheorem(goal, thm, m))
                return true;
        return false;
    }

    private boolean considerTheorem(Goal goal, Theorem thm, Mapping mapping) {
        // Construct message before applying theorem, as otherwise str(P) might evaluate to True otherwise
        String message = "By Theorem " + thm.name + " applied to (" + session.str(mapping.map(thm.data)) + "), we have " + session.str(goal.P);

        // See if we can apply the theorem using mapping
        int i = proof.size();
        List<Morphism> result = thm.apply(mapping);
        if (result == null)
            return false;

        // If the theorem was already applied, insert proof at appropriate place
        if (result.isEmpty()) {
            proof.add(i, message);
            return true;
        }

        // Otherwise, create Implication
        return createImplication(goal, result, goal.money - 1, message);
    }

    private boolean createImplication(Goal goal, List<Morphism> conditions, int money, String message) {
        // It is impossible to prove False, of course
        if (conditions.contains(session.False))
            return false;

        // If there are no (non-True) conditions, immediately conclude
        conditions.removeIf(P -> P.equals(session.True));
        if (conditions.isEmpty()) {
            proof.add(message);

            try {
                session.identify(goal.P, session.True);
            } catch (CreationException e) {
                e.printStackTrace();
            }

            return true;
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
                return false;
            else g.money = money;

            listGoals.add(g);
        }

        // Create Implication, and add goals to the queue
        Implication implication = new Implication(goal, conditions, message);
        implications.add(implication);
        queue.addAll(listGoals);
        return false;
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
