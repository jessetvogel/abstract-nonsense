package nl.jessetvogel.abstractnonsense.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class Rewriter {

    private final Session session;
    private final List<Rule> rules;

    private int KBlength;

    public Rewriter(Session session) {
        this.session = session;
        rules = new ArrayList<>();

        KBlength = 0;
    }

    public void addRule(List<Morphism> input, List<Morphism> output) {
        Rule rule = new Rule(input, output);
        rule.normalize();
        rules.add(rule);
        knuthBendix();
    }

    public boolean rewrite(List<Morphism> word) {
        // Update Knuth--Bendix length if necessary
        if(word.size() > KBlength) {
            KBlength = word.size();
            knuthBendix();
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

    private void knuthBendix() {
        List<Rule> newRules = new ArrayList<>();
        boolean updates;
        do {
            updates = false;
            newRules.clear();
            for (Rule A : rules) {
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
                        if (!X.equals(Y) && (X.size() <= KBlength && Y.size() <= KBlength)) {
                            Rule rule = new Rule(X, Y);
                            rule.normalize();
                            if(!rule.isTrivial())
                                newRules.add(rule);
                        }
                    }
                }
            }

            // Add non-trivial new rules
            if (!newRules.isEmpty()) {
                rules.addAll(newRules);
                updates = true;
            }

            // Normalize all rules & remove trivial rules
            for(Rule rule : rules)
                rule.normalize();
            rules.removeIf(Rule::isTrivial);

        } while (updates);
    }

    void replaceMorphism(Morphism f, Morphism g, List<MorphismPair> induced) {
        // Replace morphisms in rules
        for (Rule rule : rules) {
            rule.input.replaceAll(h -> (h.index == f.index ? new Morphism(g.index, h.k) : h));
            rule.output.replaceAll(h -> (h.index == f.index ? new Morphism(g.index, h.k) : h));
            rule.input.removeIf(session::isIdentity);
            rule.output.removeIf(session::isIdentity);
            rule.orient();
        }

        // Normalize rules & remove trivial rules
        for(Rule rule : rules)
            rule.normalize();
        rules.removeIf(Rule::isTrivial);

        // If any rule is now of the form 'x' -> 'y', then identify x with y
        for (Rule rule : rules) {
            if (rule.input.size() == 1 && rule.output.size() == 1)
                induced.add(new MorphismPair(rule.input.get(0), rule.output.get(0)));
        }

        knuthBendix();
    }

    private class Rule {

        List<Morphism> input;
        List<Morphism> output;
        boolean trivial;

        Rule(List<Morphism> input, List<Morphism> output) {
            this.input = input;
            this.output = output;
            orient();
            trivial = input.equals(output);
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

        boolean isTrivial() {
            return trivial || (trivial = input.equals(output));
        }

        void normalize() {
            boolean updates;
            do {
                updates = false;
                for (Rule rule : rules) {
                    if (rule == this)
                        continue;
                    if (rule.apply(input) || rule.apply(output)) {
                        orient();
                        updates = true;
                    }
                }
            } while (updates);
            trivial = input.equals(output);
        }

        private void orient() {
            if (shortlex(input, output) < 0) {
                List<Morphism> tmp = input;
                input = output;
                output = tmp;
            }
        }

        private int shortlex(List<Morphism> s, List<Morphism> t) {
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
