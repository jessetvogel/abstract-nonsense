package nl.jessetvogel.abstractnonsense.core;

public class Property {

    final String name;
    final Context context;
    final Morphism definition;

    public Property(Context context, String name, Morphism definition) {
        this.name = name;
        this.context = context;
        this.definition = definition;
    }

}
