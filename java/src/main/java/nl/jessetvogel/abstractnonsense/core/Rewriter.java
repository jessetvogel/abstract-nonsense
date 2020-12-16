package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Rewriter {

    private final Session session;
    private final List<Rule> rules;

    private int KBLength;

    public Rewriter(Session session) {
        this.session = session;
        rules = new ArrayList<>();
        KBLength = 0;
    }

    public void addRule(List<Morphism> input, List<Morphism> output) {
        Rule rule = new Rule(input, output);
        rule.normalize();
        rules.add(rule);
        knuthBendix(Set.of(rule));
    }

    public boolean rewrite(List<Morphism> word) {
        // Update Knuth--Bendix length if necessary
        if (word.size() > KBLength) {
            KBLength = word.size();
            knuthBendix(new HashSet<>(rules));
        }

        // Apply rewriting rules
        boolean updates, change = false;
        do {
            updates = false;
            for (Rule rule : rules) {
                if (rule.apply(word)) {
                    change = updates = true;
                    break;
                }
            }
        } while (updates);
        return change;
    }

    private void knuthBendix(Set<Rule> rulesToCheck) {
        Set<Rule> toCheck = new HashSet<>(rulesToCheck);
        List<Rule> newRules = new ArrayList<>();
        boolean updates;
        do {
            updates = false;
            newRules.clear();
            for (Rule A : toCheck) {
                for (Rule B : rules) {
                    int nA = A.input.size(), nB = B.input.size();
                    // Search for critical pairs
                    for (int i = 1; i < nA + nB; ++i) {
                        if (!B.input.subList(Math.max(0, nB - i), Math.min(nB, nB + nA - i))
                                .equals(A.input.subList(Math.max(0, i - nB), Math.min(nA, i))))
                            continue;

                        List<Morphism> X = null, Y = null;
                        if (i < nA && i < nB) { // Form: B (overlap) A
                            X = new ArrayList<>(B.input.subList(0, nB - i));
                            X.addAll(A.output); // Result after applying rule A
                            Y = new ArrayList<>(B.output);
                            Y.addAll(A.input.subList(i, nA)); // Result after applying rule B
                        } else if (i > nA && i > nB) { // Form: A (overlap) B
                            X = new ArrayList<>(A.output);
                            X.addAll(B.input.subList(nB + nA - i, nB)); // Result after applying rule A
                            Y = new ArrayList<>(A.input.subList(0, i - nB));
                            Y.addAll(B.output); // Result after applying rule B
                        } else if (i <= nA && i >= nB) { // Form: B contained in A
                            X = new ArrayList<>(A.output); // Result after applying rule A
                            Y = new ArrayList<>(A.input.subList(0, i - nB));
                            Y.addAll(B.output);
                            Y.addAll(A.input.subList(i, nA)); // Result after applying rule B
                        } else if (i >= nA && i <= nB) { // Form: A contained in B
                            X = new ArrayList<>(B.input.subList(0, nB - i)); // Result after applying rule A
                            X.addAll(A.output);
                            X.addAll(B.input.subList(nB + nA - i, nB));
                            Y = new ArrayList<>(B.output); // Result after applying rule B
                        }

                        assert X != null;
                        if (!X.equals(Y) && (X.size() <= KBLength && Y.size() <= KBLength)) {
                            Rule rule = new Rule(X, Y);
                            rule.normalize();
                            if (!rule.isTrivial())
                                newRules.add(rule);
                        }
                    }
                }
            }

            // Determine toCheck for next iteration: all the new/changed rules
            toCheck.clear();

            // If there are new rules, add them to 'rules' and 'toCheck' and normalize
            if (!newRules.isEmpty()) {
                rules.addAll(newRules);
                toCheck.addAll(newRules);
                for (Rule rule : rules)
                    if (rule.normalize())
                        toCheck.add(rule);
                rules.removeIf(Rule::isTrivial);
                updates = true;
            }

        } while (updates);
    }

    void replaceMorphism(Morphism f, Morphism g, List<MorphismPair> induced) throws CreationException {
        Set<Rule> changedRules = new HashSet<>();

        // Replace morphisms in rules
        for (Rule rule : rules)
            if (rule.replace(f, g))
                changedRules.add(rule);

        // Normalize rules & remove trivial rules
        for (Rule rule : rules)
            if (rule.normalize())
                changedRules.add(rule);
        rules.removeIf(Rule::isTrivial);

        // If any rule is now of the form 'x' -> 'y', then identify x with y
        for (Rule rule : rules) {
            if (rule.input.size() == 1 && rule.output.size() <= 1) {
                Morphism ff = rule.input.get(0);
                Morphism gg = rule.output.size() == 1 ? rule.output.get(0) : session.id(session.dom(ff));
                induced.add(new MorphismPair(ff, gg));
            }
        }

        // End with Knuth--Bendix
        knuthBendix(changedRules);
    }

    private class Rule {

        private List<Morphism> input;
        private List<Morphism> output;
        private boolean trivial;

        Rule(List<Morphism> input, List<Morphism> output) {
            this.input = input;
            this.output = output;
            orient();
        }

        boolean apply(List<Morphism> list) {
            if (trivial)
                return false;

            int i = Collections.indexOfSubList(list, input);
            if (i == -1)
                return false;

            list.subList(i, i + input.size()).clear();
            list.addAll(i, output);
            return true;
        }

        private boolean isTrivial() {
            return trivial;
        }

        private boolean normalize() {
            boolean change = false, updates;
            do {
                updates = false;
                for (Rule rule : rules) {
                    if (rule == this)
                        continue;
                    if (rule.apply(input) || rule.apply(output))
                        updates = change = true;
                }
            } while (updates);
            if (change)
                orient();
            return change;
        }

        private void orient() {
            int d = shortLex(input, output);
            if (d < 0) {
                List<Morphism> tmp = input;
                input = output;
                output = tmp;
            }
            if (d == 0)
                trivial = true;
        }

        private boolean replace(Morphism f, Morphism g) {
            boolean change = false;
            for (ListIterator<Morphism> it = input.listIterator(); it.hasNext(); ) {
                Morphism h = it.next();
                if (h.index == f.index) {
                    it.set(new Morphism(g.index, h.k));
                    change = true;
                }
            }
            for (ListIterator<Morphism> it = output.listIterator(); it.hasNext(); ) {
                Morphism h = it.next();
                if (h.index == f.index) {
                    it.set(new Morphism(g.index, h.k));
                    change = true;
                }
            }

            if (change) {
                input.removeIf(session::isIdentity);
                output.removeIf(session::isIdentity);
                orient();
            }

            return change;
        }

        private int shortLex(List<Morphism> s, List<Morphism> t) {
            int d = s.size() - t.size();
            if (d != 0)
                return d;
            for (int i = 0; i < s.size(); ++i) {
                d = s.get(i).index - t.get(i).index;
                if (d != 0)
                    return d;
            }
            return 0;
        }
    }

}
