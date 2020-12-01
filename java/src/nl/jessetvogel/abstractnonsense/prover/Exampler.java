package nl.jessetvogel.abstractnonsense.prover;

import nl.jessetvogel.abstractnonsense.core.Context;
import nl.jessetvogel.abstractnonsense.core.Mapping;
import nl.jessetvogel.abstractnonsense.core.Morphism;
import nl.jessetvogel.abstractnonsense.core.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Exampler {

    final Session session;
    final Context context;

    public Exampler(Session session, Context context) {
        this.session = session;
        this.context = context;
    }

    public void search() {
        Mapping mapping = new Mapping(context, session);
        List<Mapping> mappings = new ArrayList<>();
        mapping.search(mappings);
        if(mappings.isEmpty()) {
            session.print("\uD83E\uDD7A No examples found");
            return;
        }

        // Print
        for(Mapping m : mappings) {
            StringJoiner sj = new StringJoiner(", ", "{ ", " }");
            sj.setEmptyValue("{}");
            for(Morphism f : context.data) {
                Morphism g = m.map(f);
                sj.add(context.str(f) + ": " + m.target.str(g));
            }
            session.print("\uD83D\uDCD6 " + sj.toString());
        }
    }

}
