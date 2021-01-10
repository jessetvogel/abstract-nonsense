package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Context extends Diagram {

    public final List<Morphism> data;

    public Context(Session session, Diagram parent, String name) {
        super(session, parent, name);
        data = new ArrayList<>();
    }

    public String signature() {
        return session.signature(data);
    }

    public boolean isReduced() {
        // Reduced means that every morphism in the context depends on the data
        // An easy check to see if is the case is to try to map the context to itself
        Mapping mapping = createMappingFromData(this, data);
        if(mapping == null || !mapping.valid()) {
            System.err.println("Hmm, this should not happen!");
            return false;
        }
        // Now all morphisms should be mapped
        for(int index : indices) {
            if(!mapping.determined(index))
                return false;
        }
        return true;
    }

    public Mapping createMappingFromData(Diagram target, List<Morphism> targetData) {
        // Amount of data must match
        if (targetData.size() != data.size())
            return null;

        // Create Mapping from otherData
        Mapping mapping = new Mapping(this, target);
        for (int i = 0; i < data.size(); ++i) {
            if(!mapping.set(data.get(i), targetData.get(i)))
                return null;
        }

        return mapping;
    }

    @Override
    protected void replaceMorphism(Morphism f, Morphism g, List<MorphismPair> induced) throws CreationException {
        super.replaceMorphism(f, g, induced);
        data.replaceAll(z -> (z.index == f.index ? new Morphism(g.index, z.k) : z));
    }
}
