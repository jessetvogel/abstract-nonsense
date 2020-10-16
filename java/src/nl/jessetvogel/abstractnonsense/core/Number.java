package nl.jessetvogel.abstractnonsense.core;

public class Number extends Morphism {

    int n;

    Number(int n) {
        super(Global.Cat); // Cat
        this.n = n;
    }

}
