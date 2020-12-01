package nl.jessetvogel.abstractnonsense.core;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Representation {

    public enum Type {
        HOM,
        EQUALITY,
        AND,
        OR,
        COMPOSITION,
        FUNCTOR_APPLICATION,
        PROPERTY_APPLICATION
    }

    public final Type type;
    public final Property property;
    public final List<Morphism> data;

    private Representation(Type type, List<Morphism> data) {
        this.type = type;
        this.property = null;
        this.data = data;
    }

    private Representation(Property property, List<Morphism> data) {
        this.type = Type.PROPERTY_APPLICATION;
        this.property = property;
        this.data = data;
    }

    public static Representation createHom(Morphism x, Morphism y) {
        return new Representation(Type.HOM, Arrays.asList(x, y));
    }

    public static Representation createEquality(Morphism x, Morphism y) {
        return new Representation(Type.EQUALITY, Arrays.asList(x, y));
    }

    public static Representation createAnd(Morphism P, Morphism Q) {
        return new Representation(Type.AND, Arrays.asList(P, Q));
    }

    public static Representation createOr(Morphism P, Morphism Q) {
        return new Representation(Type.OR, Arrays.asList(P, Q));
    }

    public static Representation createComposition(List<Morphism> fList) {
        return new Representation(Type.COMPOSITION, fList);
    }

    public static Representation createProperty(Property property, List<Morphism> data) {
        return new Representation(property, data);
    }

    public static Representation createFunctor(Morphism F, Morphism x) {
        return new Representation(Type.FUNCTOR_APPLICATION, Arrays.asList(F, x));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        Representation other = (Representation) obj;

        if(type != other.type)
            return false;
        return switch (type) {
            case HOM, COMPOSITION, FUNCTOR_APPLICATION -> data.equals(other.data);
            case EQUALITY, AND, OR -> (data.get(0).equals(other.data.get(0)) && data.get(1).equals(other.data.get(1))) ||
                    (data.get(0).equals(other.data.get(1)) && data.get(1).equals(other.data.get(0)));
            case PROPERTY_APPLICATION -> property == other.property && data.equals(other.data);
        };
    }

    @Override
    public int hashCode() {
        return switch (type) {
            case HOM, COMPOSITION, FUNCTOR_APPLICATION -> Objects.hash(type, data);
            case EQUALITY, AND, OR -> data.get(0).hashCode() ^ data.get(1).hashCode();
            case PROPERTY_APPLICATION -> Objects.hash(type, property, data);
        };
    }

}