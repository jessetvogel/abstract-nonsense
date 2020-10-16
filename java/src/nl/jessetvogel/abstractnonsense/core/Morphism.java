package nl.jessetvogel.abstractnonsense.core;

public class Morphism {
    public Morphism category;
    public Morphism domain;
    public Morphism codomain;
    public boolean covariant;

    Morphism(Morphism category, Morphism domain, Morphism codomain, boolean covariant) {
        this.category = category;
        this.domain = domain;
        this.codomain = codomain;
        this.covariant = covariant;
    }

    Morphism(Morphism category, Morphism domain, Morphism codomain) {
        this(category, domain, codomain, true);
    }

    Morphism(Morphism category) {
        this.category = category;
        this.domain = this.codomain = this;
        this.covariant = true;
    }

    public Morphism() {
        category = domain = codomain = this;
        covariant = true;
    }

    public boolean isObject() {
        return this == domain;
    }

    public boolean isFunctor() {
        return category == Global.Cat;
    }

    public boolean isCategory() {
        return isObject() && isFunctor();
    }

}
