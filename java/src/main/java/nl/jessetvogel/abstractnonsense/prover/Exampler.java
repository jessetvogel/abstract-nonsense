package nl.jessetvogel.abstractnonsense.prover;

import nl.jessetvogel.abstractnonsense.core.*;

import java.util.*;

public class Exampler {

    final Session session;
    final Context context;

    public Exampler(Session session, Context context) {
        this.session = session;
        this.context = context;
    }

    public void search() {
        boolean results = false;

        List<Map.Entry<String, Diagram>> diagrams = new ArrayList<>(session.getExamples());
        diagrams.add(new AbstractMap.SimpleEntry<>("session", session));

        for (Map.Entry<String, Diagram> example : diagrams) {
            String name = example.getKey();
            Diagram diagram = example.getValue();

            // Search
            List<Mapping> mappings = new ArrayList<>();
            Searcher searcher = new Searcher(context, diagram);
            searcher.search(mappings);

            // Print results
            for (Mapping m : mappings) {
                // If this is an disingenuous mapping (i.e. the data is not really mapped to the target), then skip this mapping
                if(!m.target.ownsAny(m.map(context.data)))
                    continue;

                StringJoiner sj = new StringJoiner(", ");
                sj.setEmptyValue("{}");
                for (Morphism f : context.data) {
                    if (context.owns(f)) { // It might happen that some data is forced to be some value (such as True or False)
                        Morphism g = m.map(f);
                        sj.add(context.str(f) + ": " + m.target.str(g));
                    } else {
                        sj.add(m.target.str(f));
                    }
                }
                session.print("\uD83D\uDCD6 [" + name + "] " + sj.toString());
                results = true;
            }
        }

        // Print results
        if (!results) {
            session.print("\uD83E\uDD7A No examples found");
            return;
        }

    }

}
