package nl.jessetvogel.abstractnonsense.core;

public class Property {

    public final String name;
    public final Context context;
    public final Morphism definition;

    public Property(Context context, String name, Morphism definition) {
        this.name = name;
        this.context = context;
        this.definition = definition;
    }

}
