package nl.jessetvogel.abstractnonsense.prover;

import nl.jessetvogel.abstractnonsense.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class Exampler {

    final Session session;
    final Context context;

    public Exampler(Session session, Context context) {
        this.session = session;
        this.context = context;
    }

    public void search() {
        boolean results = false;

        for (Map.Entry<String, Diagram> example : session.getExamples()) {
            String name = example.getKey();
            Diagram diagram = example.getValue();

            // Search
            List<Mapping> mappings = new ArrayList<>();
            Searcher searcher = new Searcher(context, diagram);
            searcher.search(mappings);

            // Print results
            for (Mapping m : mappings) {
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