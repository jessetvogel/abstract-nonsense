package nl.jessetvogel.abstractnonsense.core;

public class Morphism {

    public final int index;
    public final int k;
    public MorphismInfo info;

    Morphism(int index, int k) {
        this.index = index;
        this.k = k;
        info = null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Morphism))
            return false;

        return index == ((Morphism) o).index && k == ((Morphism) o).k;
    }

    @Override
    public int hashCode() {
        return (index << 16) + k;
    }
}

