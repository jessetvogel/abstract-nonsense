package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Session extends Diagram {

    private int indexCounter = 0;
    private int indexTopCat = -1;

    private boolean contradiction;

    private final Map<String, Property> properties;
    private final Map<String, Theorem> theorems;
    private final Map<String, Diagram> examples;

    private final Map<Integer, Diagram> owners;
    private final Map<Integer, MorphismInfo> morphismInfo;
    private final Map<Integer, Integer> identifications;

    public final List<Integer> nCat;
    public final Morphism True, False, Prop, Set, Cat;

    public Session() {
        super(null, null, "session");

        // Create maps
        properties = new HashMap<>();
        theorems = new HashMap<>();
        examples = new HashMap<>();
        owners = new HashMap<>();
        morphismInfo = new HashMap<>();
        identifications = new HashMap<>();
        contradiction = false;

        // Create default morphisms
        nCat = new ArrayList<>();

        // Create category of categories
        True = newTopCat();
        Prop = newTopCat();
        Set = newTopCat();
        Cat = newTopCat();

        // Create False
        Morphism F = null;
        try {
            F = createObject(this, Prop);
        } catch (CreationException e) {
            // This can't happen..
            assert false;
        }
        False = F;

        // Define some symbols
        assignSymbol("Cat", Cat);
        assignSymbol("Set", Set);
        assignSymbol("True", True);
        assignSymbol("Prop", Prop);
        assignSymbol("False", False);
    }

    public boolean contradiction() {
        return contradiction;
    }

    private Morphism newTopCat() {
        // C denotes the category of n-categories with n the currently highest <insert word here>
        Morphism C = new Morphism(indexCounter++, 0);
        owners.put(C.index, this);
        addIndex(C.index);
        nCat.add(C.index);
        if (indexTopCat != -1)
            morphismInfo.put(indexTopCat, new MorphismInfo(C.index, 0, C.index, C.index));
        indexTopCat = C.index;
        return C;
    }

    public int degree(Morphism x) {
        return degree(x.index) - x.k;
    }

    public int degree(Integer index) {
        int m = nCat.indexOf(index);
        if (m != -1)
            return m - 1;
        MorphismInfo info = morphismInfo.get(index);
        return degree(info.cat) - info.k - 1;
    }

    private void morphismSetInfo(Morphism f) {
        if (f.info != null)
            return;
        if (f.index == indexTopCat)
            newTopCat();
        f.info = morphismInfo.get(f.index);
        if(f.info == null)
            System.err.println("Cannot find morphism info for index " + f.index + "!");
    }

    public Morphism cat(Morphism f) {
        morphismSetInfo(f);
        return new Morphism(f.info.cat, 0);
    }

    public Morphism dom(Morphism f) {
        morphismSetInfo(f);
        if (f.k == f.info.k)
            return new Morphism(f.info.dom, f.k - 1);
        else
            return new Morphism(f.index, f.k - 1);
    }

    public Morphism cod(Morphism f) {
        morphismSetInfo(f);
        if (f.k == f.info.k)
            return new Morphism(f.info.cod, f.k - 1);
        else
            return new Morphism(f.index, f.k - 1);
    }

    public Morphism id(Morphism f) throws CreationException {
        morphismSetInfo(f);
        int n = degree(f.info.cat);
        if (f.k >= n)
            throw new CreationException("Cannot create " + (f.k + 1) + "-morphisms in a " + n + "-category");

        return new Morphism(f.index, f.k + 1);
    }

    public Morphism createObject(Diagram diagram, Morphism category) throws CreationException {
        // Allocate a new index for this morphism and set info
        int index = indexCounter++;
        morphismInfo.put(index, new MorphismInfo(category.index, 0, category.index, category.index));
        owners.put(index, diagram);
        diagram.addIndex(index);
        return new Morphism(index, 0);
    }

    public Morphism createMorphism(Diagram diagram, Morphism domain, Morphism codomain) throws CreationException {
        // For a morphism from domain to codomain, the domain and codomain must be comparable
        if(!comparable(domain, codomain))
            throw new CreationException("domain and codomain are not comparable");

        // k must be at most the n-value of the category
        Morphism category = session.cat(domain);
        int n = session.degree(category);
        int k = domain.k + 1;
        if (k > n)
            throw new CreationException("cannot create a " + k + "-morphism in a " + n + "-category");

        // If all is verified, allocate a new index for this morphism and set info
        int index = indexCounter++;
        morphismInfo.put(index, new MorphismInfo(category.index, k, domain.index, codomain.index));
        owners.put(index, diagram);
        diagram.addIndex(index);
        return new Morphism(index, k);
    }

    public void addProperty(Property prop) {
        properties.put(prop.name + prop.context.signature(), prop);
    }

    public boolean hasProperty(String name, String signature) {
        return properties.containsKey(name + signature);
    }

    public Property getProperty(String name, String signature) {
        return properties.get(name + signature);
    }

    public void addTheorem(Theorem thm) {
        theorems.put(thm.name, thm);
    }

    public boolean hasTheorem(String name) {
        return theorems.containsKey(name);
    }

    public Theorem getTheorem(String name) {
        return theorems.get(name);
    }

    public Collection<Theorem> getTheorems() {
        return theorems.values();
    }

    public boolean hasExample(String name) { return examples.containsKey(name); }

    public void addExample(String name, Diagram example) {
        examples.put(name, example);
    }

    public Collection<Diagram> getExamples() { return examples.values(); }

    public Diagram getExample(String name) {
        return examples.get(name);
    }

    public Morphism nCat(int n) {
        int i = n + 2; // Mind the offset
        while (i >= nCat.size())
            newTopCat();
        return new Morphism(nCat.get(i), 0);
    }

    public boolean isIdentity(Morphism f) {
        morphismSetInfo(f);
        return f.k > f.info.k;
    }

    public boolean isCategory(Morphism C) {
        return C.k == 0 && nCat.contains(cat(C).index);
    }

    public void identify(Morphism f, Morphism g) throws CreationException {
        // First look up in the identification table
        while(identifications.containsKey(f.index))
            f = new Morphism(identifications.get(f.index), f.k);
        while(identifications.containsKey(g.index))
            g = new Morphism(identifications.get(g.index), g.k);

        // Detect contradictions
        if((f.equals(True) && g.equals(False)) || (f.equals(False) && g.equals(True))) {
            contradiction = true;
            return;
        }

        // If f and g are already equal, we are done
        if (f.equals(g))
            return;

        // f and g must be comparable
        if (!comparable(f, g))
            throw new CreationException("Incomparable morphisms");

        // We are going to replace f with g, so f must be the in the 'younger' diagram, and g in the 'older' diagram.
        // In other words, owner(f) must know g. If not, swap f and g
        Diagram df = owner(f), dg = owner(g);
        if((df == dg && f.index < g.index) || !df.knows(g)) { // TODO: think about this strategy once more.. does it work?
            Morphism z = f;
            f = g;
            g = z;
            df = owner(f); // Update owner
        }

        // Make changes in MorphismInfo
        for (MorphismInfo info : morphismInfo.values()) {
            if (info.cat == f.index) info.cat = g.index;
            if (info.dom == f.index) info.dom = g.index;
            if (info.cod == f.index) info.cod = g.index;
        }

        // Now we replace f with g (keeping track of induced identifications) starting from the Diagram that owns f (any Diagram below that won't know of f)
        List<MorphismPair> inducedIdentifications = new ArrayList<>();
        df.replaceMorphism(f, g, inducedIdentifications);

        // Delete index from diagram and remove owner
        df.removeIndex(f.index);
        owners.remove(f.index);
        morphismInfo.remove(f.index);

        // If g is True, then there might be some more induced identifications
        if (g.equals(True) || g.equals(False)) {
            boolean gBool = g.equals(True);
            for (Representation rep : df.getRepresentations(g)) {
                switch (rep.type) {
                    case HOM: {
                        Morphism P = rep.data.get(0), Q = rep.data.get(1);
                        if (!gBool) {
                            inducedIdentifications.add(new MorphismPair(P, True));
                            inducedIdentifications.add(new MorphismPair(Q, False));
                        }
                        if (gBool && Q.equals(False))
                            inducedIdentifications.add(new MorphismPair(P, False));
                        break;
                    }
                    case EQUALITY: {
                        if (gBool) {
                            Morphism u = rep.data.get(0), v = rep.data.get(1);
                            if(cat(u).equals(Prop) || !isCategory(u))
                                inducedIdentifications.add(new MorphismPair(u, v));
                        }
                        break;
                    }
                    case AND: {
                        if (gBool) {
                            inducedIdentifications.add(new MorphismPair(rep.data.get(0), True));
                            inducedIdentifications.add(new MorphismPair(rep.data.get(1), True));
                        }
                        break;
                    }
                    case OR: {
                        if (!gBool) {
                            inducedIdentifications.add(new MorphismPair(rep.data.get(0), False));
                            inducedIdentifications.add(new MorphismPair(rep.data.get(1), False));
                        }
                    }
                    default:
                        break;
                }
            }
        }

        // Set identifications
        identifications.put(f.index, g.index);

        for (MorphismPair pair : inducedIdentifications)
            identify(pair.f, pair.g);
    }

    public boolean comparable(Morphism f, Morphism g) {
        if (f.k != g.k)
            return false;
        if (!cat(f).equals(cat(g)))
            return false;
        return f.k == 0 || (dom(f).equals(dom(g)) && cod(f).equals(cod(g)));
    }

    public Diagram owner(Morphism f) {
        return owners.get(f.index);
    }

    public Morphism morphismFromIndex(int index) {
        MorphismInfo info = morphismInfo.get(index);
        if (info == null)
            return null;

        Morphism f = new Morphism(index, info.k);
        f.info = info;
        return f;
    }

    public Morphism morphismFromIndex(int index, int k) {
        MorphismInfo info = morphismInfo.get(index);
        if (info == null)
            return null;

        if(info.k > k)
            return null;

        Morphism f = new Morphism(index, k);
        f.info = info;
        return f;
    }

    // -------- Stringify --------

    public String strList(List<Morphism> list) {
        if (list.isEmpty())
            return "";
        StringJoiner sj = new StringJoiner(", ");
        for (Morphism x : list)
            sj.add(str(x));
        return sj.toString();
    }

    public String str(Representation rep) {
        switch (rep.type) {
            case HOM:
                return rep.data.get(1).equals(session.False) ? "~" + wrap(str(rep.data.get(0))) : (wrap(str(rep.data.get(0))) + " -> " + wrap(str(rep.data.get(1))));
            case EQUALITY:
                return wrap(str(rep.data.get(0))) + " = " + wrap(str(rep.data.get(1)));
            case AND:
                return wrap(str(rep.data.get(0))) + " & " + wrap(str(rep.data.get(1)));
            case OR:
                return wrap(str(rep.data.get(0))) + " | " + wrap(str(rep.data.get(1)));
            case COMPOSITION: {
                StringJoiner sj = new StringJoiner(".");
                for (Morphism f : rep.data)
                    sj.add(wrap(str(f)));
                return sj.toString();
            }
            case FUNCTOR_APPLICATION:
                return wrap(str(rep.data.get(0))) + "(" + str(rep.data.get(1)) + ")";
            case PROPERTY_APPLICATION:
                return rep.property.name + "(" + strList(rep.data) + ")";
            default:
                return null;
        }
    }

    public String str(Morphism f) {
        // For identity morphisms
        if (session.isIdentity(f))
            return "id(" + str(new Morphism(f.index, f.k - 1)) + ")";

        // Preferably use symbols
        Diagram owner = owner(f);
        for (Map.Entry<String, Morphism> entry : owner.symbols.entrySet())
            if (entry.getValue().equals(f))
                return (owner == session || owner.name.equals("") ? "" : owner.name + "::") + entry.getKey();

        // Use the first-defined representation
        for (Map.Entry<Representation, Morphism> entry : owner.representations.entrySet())
            if (entry.getValue().equals(f))
                return str(entry.getKey());

        return "[" + f.index + "]";
    }

    private String wrap(String s) {
        if (s.matches("^\\w*(\\(.*\\))?$"))
            return s;
        else
            return "(" + s + ")";
    }

    public String signature(List<Morphism> list) {
        StringJoiner sj = new StringJoiner(",", "(", ")");
        for (Morphism f : list)
            sj.add(String.valueOf(f.k));
        return sj.toString();
    }

}
