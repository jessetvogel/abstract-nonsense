package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Context extends Diagram {

    ArrayList<Morphism> data, conditions;

    Context(Diagram parent) {
        super(parent);
        data = new ArrayList<>();
        conditions = new ArrayList<>();
    }

    public void addData(Morphism x) {
        data.add(x);
    }

    public void addCondition(Morphism x) {
        conditions.add(x);
        // Automatically add a proof of condition to the context
        addMorphism(createObject(x));
    }

    public boolean isData(Morphism x) {
        return data.contains(x);
    }

    public boolean validateData(Diagram target, ArrayList<Morphism> targetData) {
        Mapping mapping = mappingFromData(target, targetData);
        if(mapping == null)
            return false;
        return mapping.validate();
    }

    public Mapping mappingFromData(Diagram target, ArrayList<Morphism> targetData) {
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
}
