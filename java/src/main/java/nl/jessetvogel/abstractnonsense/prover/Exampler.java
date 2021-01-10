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

    public List<Mapping> search() {
        List<Mapping> mappings = new ArrayList<>();

        Set<Diagram> diagrams = new HashSet<>(session.getExamples());
        diagrams.add(session);
        for (Diagram diagram : diagrams) {
            // Search
            Searcher searcher = new Searcher(context, diagram);
            searcher.search(mappings);
        }

        return mappings;

//            // Print results
//            for (Mapping m : mappings) {
//                // If this is an disingenuous mapping (i.e. the data is not really mapped to the target), then skip this mapping
//                if(!m.target.ownsAny(m.map(context.data)))
//                    continue;
//
//                StringJoiner sj = new StringJoiner(", ");
//                sj.setEmptyValue("{}");
//                for (Morphism f : context.data) {
//                    if (context.owns(f)) { // It might happen that some data is forced to be some value (such as True or False)
//                        Morphism g = m.map(f);
//                        sj.add(session.str(f) + ": " + session.str(g));
//                    } else {
//                        sj.add(session.str(f));
//                    }
//                }
//                results.add("[" + name + "] " + sj.toString());
//            }
//        return results;
    }

}
