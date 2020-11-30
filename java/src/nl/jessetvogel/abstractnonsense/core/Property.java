package nl.jessetvogel.abstractnonsense.core;

import java.util.List;

public class Property {

    final String name;
    final Context context;
    final Morphism definition;

    public Property(Context context, String name) {
        this(context, name, null);
    }

    public Property(Context context, String name, Morphism definition) {
        this.name = name;
        this.context = context;
        this.definition = definition;
    }

}
