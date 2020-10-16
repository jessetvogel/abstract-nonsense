package nl.jessetvogel.abstractnonsense.core;

public class Property extends Context {

    String name;

    public Property(Diagram parent, String name) {
        super(parent);
        this.name = name;
    }

}
