package nl.jessetvogel.abstractnonsense.core;

public class Theorem extends Context {

    String name;
    Diagram conclusion;

    public Theorem(Diagram parent, String name) {
        super(parent);
        this.name = name;
        conclusion = new Diagram(this);
    }

    boolean tryApplication(Diagram other) {
        return false;
    }

    public Diagram getConclusion() {
        return conclusion;
    }
}
