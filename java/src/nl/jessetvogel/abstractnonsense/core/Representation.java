package nl.jessetvogel.abstractnonsense.core;

import java.util.ArrayList;
import java.util.Objects;

public class Representation {

    public enum Type {
        COMPOSITION,
        FUNCTOR_APPLICATION,
        PROPERTY_APPLICATION
    }

    public Type type;
    public Property property;
    public ArrayList<Morphism> data;

    Representation(Type type, ArrayList<Morphism> data) {
        this.type = type;
        this.property = null;
        this.data = data;
    }

    Representation(Type type, Property property, ArrayList<Morphism> data) {
        this.type = type;
        this.property = property;
        this.data = data;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        Representation rep = (Representation) obj;
        return this.type == rep.type && this.data.equals(rep.data) && this.property == rep.property;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, property, data);
    }

}
