package nl.jessetvogel.abstractnonsense.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Book extends Diagram {

    Map<String, Property> properties;
    Map<String, Theorem> theorems;

    public Book(Diagram parent) {
        super(parent);
        properties = new HashMap<>();
        theorems = new HashMap<>();
    }

    public void addProperty(Property prop) {
        properties.put(prop.name, prop);
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public void addTheorem(Theorem thm) {
        theorems.put(thm.name, thm);
    }

    public boolean hasTheorem(String name) {
        return theorems.containsKey(name);
    }

    public Property getProperty(String name) {
        Property prop = properties.get(name);
        if(prop != null)
            return prop;
        return super.getProperty(name);
    }

    public Theorem getTheorem(String name) {
        Theorem thm = theorems.get(name);
        if(thm != null)
            return thm;
        return super.getTheorem(name);
    }

    public boolean owns(Property property) {
        return properties.containsValue(property);
    }

    public Collection<Theorem> getTheorems() {
        return theorems.values();
    }

}
