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
        return this.type == other.type && this.property == other.property && this.data.equals(other.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, property, data);
    }

}