package nl.jessetvogel.abstractnonsense.core;

import java.util.Arrays;
import java.util.List;

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

    public Representation map(Mapping m) {
        switch (type) {
            case HOM:
                return hom(m.map(data.get(0)), m.map(data.get(1)));
            case EQUALITY:
                return equality(m.map(data.get(0)), m.map(data.get(1)));
            case AND:
                return and(m.map(data.get(0)), m.map(data.get(1)));
            case OR:
                return or(m.map(data.get(0)), m.map(data.get(1)));
            case COMPOSITION:
                return composition(m.map(data));
            case FUNCTOR_APPLICATION:
                return functorApplication(m.map(data.get(0)), m.map(data.get(1)));
            case PROPERTY_APPLICATION:
                return propertyApplication(property, m.map(data));
            default:
                System.err.println("Whoops! What happened?");
                return null;
        }
    }

    public static Representation hom(Morphism x, Morphism y) {
        return new Representation(Type.HOM, Arrays.asList(x, y));
    }

    public static Representation equality(Morphism x, Morphism y) {
        return new Representation(Type.EQUALITY, Arrays.asList(x, y));
    }

    public static Representation and(Morphism P, Morphism Q) {
        return new Representation(Type.AND, Arrays.asList(P, Q));
    }

    public static Representation or(Morphism P, Morphism Q) {
        return new Representation(Type.OR, Arrays.asList(P, Q));
    }

    public static Representation composition(List<Morphism> fList) {
        return new Representation(Type.COMPOSITION, fList);
    }

    public static Representation propertyApplication(Property property, List<Morphism> data) {
        return new Representation(property, data);
    }

    public static Representation functorApplication(Morphism F, Morphism f) {
        return new Representation(Type.FUNCTOR_APPLICATION, Arrays.asList(F, f));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        Representation other = (Representation) obj;

        if (type != other.type)
            return false;
        switch (type) {
            case HOM:
            case COMPOSITION:
            case FUNCTOR_APPLICATION:
                return data.equals(other.data);
            case EQUALITY:
            case AND:
            case OR:
                return (data.get(0).equals(other.data.get(0)) && data.get(1).equals(other.data.get(1))) ||
                        (data.get(0).equals(other.data.get(1)) && data.get(1).equals(other.data.get(0)));
            case PROPERTY_APPLICATION:
                return property == other.property && data.equals(other.data);
            default:
                return false;
        }
    }

    @Override
    public int hashCode() {
        switch (type) {
            case HOM:
                return 0x25afd075 ^ data.hashCode();
            case COMPOSITION:
                return 0xf3e9e500 ^ data.hashCode();
            case FUNCTOR_APPLICATION:
                return 0xecf348f4 ^ (31 * data.get(0).hashCode()) + data.get(1).hashCode();
            case PROPERTY_APPLICATION:
                assert property != null;
                return 0xce870ca1 ^ property.hashCode() ^ data.hashCode();
            case EQUALITY:
            case AND:
            case OR:
                int h1 = data.get(0).hashCode();
                int h2 = data.get(1).hashCode();
                return 0xe9fc74ac ^ type.hashCode() ^ ((h1 ^ h2) + 17 * (h1 + h2));
        }
        return 0;
    }

}