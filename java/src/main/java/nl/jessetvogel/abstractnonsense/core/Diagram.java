package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Diagram {

    protected final Session session;
    private final Diagram parent;
    private final List<Diagram> children;

    public final List<Integer> indices;
    public final Map<Representation, Morphism> representations;
    private final Map<String, Morphism> symbols;
    private final Rewriter rewriter;

    public Diagram(Session session, Diagram parent) {
        this.session = (session != null ? session : (Session) this);
        this.parent = parent;

        indices = new ArrayList<>();
        symbols = new LinkedHashMap<>();
        representations = new LinkedHashMap<>();
        rewriter = new Rewriter(this.session);

        if (hasParent())
            parent.children.add(this);
        children = new ArrayList<>();
    }

    public boolean hasParent() {
        return parent != null;
    }

    public Diagram getParent() {
        return parent;
    }

    public void detach() {
        if (hasParent())
            parent.children.remove(this);
    }

    public void assignSymbol(String name, Morphism x) {
        symbols.put(name, x);
    }

    public boolean hasSymbol(String name) {
        return symbols.containsKey(name);
    }

    public Morphism getMorphism(String name) {
        if (hasSymbol(name))
            return symbols.get(name);
        if (hasParent())
            return parent.getMorphism(name);
        return null;
    }

    public List<Representation> getRepresentations(Morphism f) {
        List<Representation> reps = new ArrayList<>();
        for (Map.Entry<Representation, Morphism> entry : representations.entrySet()) {
            if (entry.getValue().equals(f))
                reps.add(entry.getKey());
        }
        if (hasParent() && !owns(f))
            reps.addAll(parent.getRepresentations(f));
        return reps;
    }

    public boolean owns(Morphism f) {
        return indices.contains(f.index);
    }

    public boolean owns(int index) {
        return indices.contains(index);
    }

    public boolean ownsAny(List<Morphism> list) {
        for (Morphism x : list)
            if (owns(x))
                return true;
        return false;
    }

    private Diagram owner(List<Morphism> list) {
        if (!hasParent() || ownsAny(list))
            return this;
        return parent.owner(list);
    }

    boolean knows(Morphism x) {
        if (owns(x))
            return true;
        if (hasParent())
            return parent.knows(x);
        return false;
    }

    protected void replaceMorphism(Morphism f, Morphism g, List<MorphismPair> induced) throws CreationException {
        // Replace in rewriter
        rewriter.replaceMorphism(f, g, induced);

        // Replace symbol pointers
        for (Map.Entry<String, Morphism> entry : symbols.entrySet()) {
            Morphism h = entry.getValue();
            if (h.index == f.index)
                entry.setValue(new Morphism(g.index, h.k));
        }

        // Replace representation pointers
        for (Map.Entry<Representation, Morphism> entry : representations.entrySet()) {
            Morphism h = entry.getValue();
            if (h.index == f.index)
                entry.setValue(new Morphism(g.index, h.k));
        }

        // If a representation contains f as data, recreate it
        for (Map.Entry<Representation, Morphism> entry : new HashSet<>(representations.entrySet())) {
            Representation rep = entry.getKey();

            boolean flag = false; // TODO: Can this be shortened?
            for (Morphism h : rep.data) {
                if (h.index == f.index) {
                    flag = true;
                    break;
                }
            }
            if (!flag)
                continue;

            representations.remove(rep);
            rep.data.replaceAll(h -> (h.index == f.index ? new Morphism(g.index, h.k) : h));
            Morphism h = morphism(rep);
            induced.add(new MorphismPair(entry.getValue(), h));
        }

        // Also make replacements in children
        for (Diagram child : children)
            child.replaceMorphism(f, g, induced);
    }

    // --- Creation methods ---

    public Morphism morphism(Representation rep) throws CreationException {
        // If this diagram does not own any of the data, let its parent create it
        if (hasParent() && !ownsAny(rep.data))
            return parent.morphism(rep);

        switch (rep.type) {
            case HOM: return createHom(rep);
            case EQUALITY: return createEquality(rep);
            case AND: return createAnd(rep);
            case OR: return createOr(rep);
            case COMPOSITION: return createComposition(rep);
            case FUNCTOR_APPLICATION: return createFunctorApplication(rep);
            case PROPERTY_APPLICATION: return createPropertyApplication(rep);
            default: return null;
        }
    }

    private Morphism createHom(Representation rep) throws CreationException {
        Morphism f = rep.data.get(0), g = rep.data.get(1);

        // Morphisms f and g must be comparable
        if (!session.comparable(f, g))
            throw new CreationException("Given morphisms are not comparable");

        // Trivial cases
        if (session.cat(f).equals(session.Prop)) {
            if (f.equals(session.True))
                return g;
            if (g.equals(session.True))
                return session.True;
            if (g.equals(f))
                return session.True;
        }

        // Lookup representation
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Create Hom-morphism
        int n = session.degree(f);
        Morphism h = session.createObject(this, session.nCat(session.degree(f)));
        representations.put(rep, h);
        if (n >= 0)
            session.setHom(h.index, f, g); // TODO: this is a sort of workaround, don't know how to do it cleaner..

        // If g equals False, we are talking about a negation: automatically identify ~~P with P
        if (g.equals(session.False)) {
            Representation repNegation = Representation.hom(h, session.False);
            representations.put(repNegation, f);
        }

        return h;
    }

    private Morphism createEquality(Representation rep) throws CreationException {
        Morphism f = rep.data.get(0), g = rep.data.get(1);

        // Morphisms f and g must be comparable
        if (!session.comparable(f, g))
            throw new CreationException("Given morphisms are not comparable");

        // Trivial cases
        if (f.equals(g))
            return session.True;

        // Lookup representation
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Create Proposition
        Morphism P = session.createObject(this, session.Prop);
        representations.put(rep, P);
        return P;
    }

    private Morphism createAnd(Representation rep) throws CreationException {
        Morphism P = rep.data.get(0), Q = rep.data.get(1);

        // P and Q must be Propositions
        if (!session.cat(P).equals(session.Prop) || !session.cat(Q).equals(session.Prop))
            throw new CreationException("Operation '&' only applies to propositions");

        // Trivial cases
        if (P.equals(session.False) || Q.equals(session.False))
            return session.False;
        if (P.equals(session.True))
            return Q;
        if (Q.equals(session.True))
            return P;
        if (P.equals(Q))
            return P;

        // Lookup representation
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Create Proposition
        Morphism R = session.createObject(this, session.Prop);
        representations.put(rep, R);

        // Automatically put (P & Q) -> P and (P & Q) -> Q to True
        representations.put(Representation.hom(R, P), session.True);
        representations.put(Representation.hom(R, Q), session.True);

        return R;
    }

    private Morphism createOr(Representation rep) throws CreationException {
        Morphism P = rep.data.get(0), Q = rep.data.get(1);

        // P and Q must be Propositions
        if (!session.cat(P).equals(session.Prop) || !session.cat(Q).equals(session.Prop))
            throw new CreationException("Operation '|' only applies to propositions");

        // Trivial cases
        if (P.equals(session.True) || Q.equals(session.True))
            return session.True;
        if (P.equals(session.False))
            return Q;
        if (Q.equals(session.False))
            return P;
        if (P.equals(Q))
            return P;

        // Lookup representation
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Create Proposition
        Morphism R = session.createObject(this, session.Prop);
        representations.put(rep, R);

        // Automatically put P -> (P | Q) and Q -> (P | Q) to True
        representations.put(Representation.hom(P, R), session.True);
        representations.put(Representation.hom(Q, R), session.True);

        return R;
    }

    private Morphism createComposition(Representation rep) throws CreationException {
        List<Morphism> list = rep.data;

        // There must be at least one morphism
        if (list.isEmpty())
            throw new CreationException("There must be at least one morphism");

        // Determine relevant domain and codomain
        int n = list.size();
        Morphism x = session.dom(list.get(list.size() - 1));
        Morphism y = session.cod(list.get(0));

        // The morphisms must connect (this automatically makes sure all morphisms are in the same category, and that the k-values are okay)
        for (int i = 0; i < n - 1; ++i) {
            if (!session.dom(list.get(i)).equals(session.cod(list.get(i + 1))))
                throw new CreationException("Given morphisms do not connect well");
        }

        // Simply remove all identity morphisms
        list.removeIf(session::isIdentity);

        // Make a copy of the list, that will be used for the representation later on
        List<Morphism> copy = new ArrayList<>(list);

        // Apply rewriter of this and its parents TODO: alternatively, link the plural rewriter!
        boolean updates;
        do {
            updates = false;
            Diagram diagram = this;
            while (diagram != null) {
                if (diagram.rewriter.rewrite(list)) {
                    updates = true;
                    break;
                }
                diagram = diagram.parent;
            }
        } while (updates);

        // If the list is empty now, then the result would have been id_x = id_y
        n = list.size();
        if (n == 0)
            return session.id(x);

        // If there is only one morphism left, just return that morphism
        if (n == 1) {
            representations.put(Representation.composition(copy), list.get(0));
            return list.get(0);
        }

        // TODO: If after rewriting we don't own any of the morphisms, again let the parent create this
//        if (hasParent() && !ownsAny(list))
//            return parent.createComposition(rep);

        // If there is still more than one morphism, this composition is not yet created.
        // So we create it now, and set the representation. Also, add a new rewrite rule.
        Morphism g = session.createMorphism(owner(list), session.cat(list.get(0)), list.get(0).k, x, y);
        representations.put(rep, g);
        rewriter.addRule(new ArrayList<>(list), new ArrayList<>(Collections.singleton(g)));
        return g;
    }

    private Morphism createPropertyApplication(Representation rep) throws CreationException {
        Property property = rep.property;
        List<Morphism> data = rep.data;

        // The data must fit in the context of the property
        Mapping mapping = property.context.createMappingFromData(this, data);
        if (mapping == null || !mapping.valid())
            throw new CreationException("Property does not apply to the given data");

        // Lookup representation
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Use property definition if exists, otherwise create proposition
//        representations.put(rep, null); // This is so that this bucket comes before the property definition
        Morphism P = (property.definition != null) ? mapping.map(property.definition) : session.createObject(this, session.Prop);
        representations.put(rep, P);
        return P;
    }

    private Morphism createFunctorApplication(Representation rep) throws CreationException {
        Morphism F = rep.data.get(0), f = rep.data.get(1);

        // TODO: I don't quite know what the general structure would be, so for now just do this case-wise

        // Lookup representation
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Look for alternative representations: if f is of the form G(g), then F(f) = (FG)(g)
        for (Representation ry : getRepresentations(f)) {
            if (ry.type != Representation.Type.FUNCTOR_APPLICATION)
                continue;

            Morphism G = ry.data.get(0);
            Morphism g = ry.data.get(1);

            Morphism FG = morphism(Representation.composition(new ArrayList<>(Arrays.asList(F, G))));
            return morphism(Representation.functorApplication(FG, g));
        }

        Morphism g = null;

        // If F is a 1-morphism in Set, it maps elements between sets
        if (session.cat(F).equals(session.Set) && F.k == 1) {
            // f must be a 0-morphism in 1-dom(F)
            if (!session.cat(f).equals(session.dom(F)) || f.k != 0)
                throw new CreationException("Function not applicable to given input");

            // If F is the identity map, just return f
            if (session.isIdentity(F))
                return f;

            // Otherwise, create an element in cod(F)
            g = session.createObject(this, session.cod(F));
        }

        // If F is a 1-morphism in Cat, it maps objects to objects, and morphisms to morphisms
        if (session.cat(F).equals(session.Cat) && F.k == 1) {
            // f must be a 0-morphism or 1-morphism in 1-dom(F)
            if ((f.k != 0 && f.k != 1) || !session.cat(f).equals(session.dom(F)))
                throw new CreationException("Functor not applicable to given input");

            // If F is the identity map, just return f
            if (session.isIdentity(F))
                return f;

            // If f is a 0-morphism, F(f) is a 0-morphism in 1-cod(F)
            if (f.k == 0)
                g = session.createObject(this, session.cod(F));

            // If f is a 1-morphism a -> b, F(f) is a 1-morphism F(a) -> F(b) in 1-cod(F)
            if (f.k == 1) {
                Morphism X = session.dom(f), Y = session.cod(f);
                Morphism FX = createFunctorApplication(Representation.functorApplication(F, X));
                Morphism FY = createFunctorApplication(Representation.functorApplication(F, Y));
                g = session.createMorphism(this, session.cod(F), 1, FX, FY);
            }
        }

        if (g == null)
            throw new CreationException("Don't know about this kind of functor construction yet?");

        representations.put(rep, g);
        return g;
    }

    // ------------ Stringify ------------

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
        if(hasParent() && !owns(f))
            return parent.str(f);

        // For identity morphisms
        if (session.isIdentity(f))
            return "id(" + str(new Morphism(f.index, f.k - 1)) + ")";

        // Preferably use symbols
        for (Map.Entry<String, Morphism> entry : symbols.entrySet())
            if (entry.getValue().equals(f))
                return entry.getKey();

        // Use the first-defined representation
        for (Map.Entry<Representation, Morphism> entry : representations.entrySet())
            if (entry.getValue().equals(f))
                return str(entry.getKey());

        if (hasParent() && !owns(f))
            return parent.str(f);

        return "[" + f.index + "]";
    }

    private String wrap(String s) {
        if (s.matches("^\\w*(\\(.*\\))?$"))
            return s;
        else
            return "(" + s + ")";
    }

    protected void addIndex(int index) {
        indices.add(index);
    }

    protected void removeIndex(Integer index) {
        indices.remove(index);
    }

    public Session getSession() {
        return session;
    }
}
