package nl.jessetvogel.abstractnonsense.core;

public class MorphismInfo {

    public final int k;
    public int cat, dom, cod;

    public MorphismInfo(int cat, int k, int dom, int cod) {
        this.k = k;
        this.cat = cat;
        this.dom = dom;
        this.cod = cod;
    }

}
