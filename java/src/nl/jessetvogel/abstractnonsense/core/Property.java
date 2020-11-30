package nl.jessetvogel.abstractnonsense.core;

public class Property extends Context {

    String name;

    public Property(Session session, String name) {
        super(session, session);
        this.name = name;
    }

}
