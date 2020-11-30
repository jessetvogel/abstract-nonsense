package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public abstract class Context extends Diagram {

    List<Morphism> data;

    Context(Session session, Diagram parent) {
        super(session, parent);
        data = new ArrayList<>();
    }

    public void addData(Morphism x) {
        data.add(x);
    }

    public boolean isData(Morphism x) {
        return data.contains(x);
    }

    public boolean isValidData(Diagram target, List<Morphism> targetData) {
        Mapping mapping = mappingFromData(target, targetData);
        if(mapping == null)
            return false;
        return mapping.isValid();
    }

    public boolean isReduced() {
        // Reduced means that every morphism in the context depends on the data
        // An easy check to see if is the case is to try to map the context to itself
        Mapping mapping = mappingFromData(this, data);
        if(mapping == null || !mapping.isValid()) {
            System.err.println("Hmm, this should not happen!");
            return false;
        }
        // Now all morphisms should be mapped
        for(int i : indices) {
            if(!mapping.maps(i))
                return false;
        }
        return true;
    }

    public Mapping mappingFromData(Diagram target, List<Morphism> targetData) {
        // Amount of data must match
        if (targetData.size() != data.size())
            return null;

        // Create Mapping from otherData
        Mapping mapping = new Mapping(this, target);
        for (int i = 0; i < data.size(); ++i) {
            if(!mapping.set(data.get(i), targetData.get(i))) {
                System.out.println("Could not map ... to ...");
                return null;
            }
        }

        return mapping;
    }

//    protected void replaceMorphism(Morphism x, Morphism y, List<InducedEquality> induced) throws CreationException {
//        super.replaceMorphism(x, y, induced);
//        if (data.contains(x))
//            data.replaceAll(z -> (z == x ? y : z));
//    }
}
