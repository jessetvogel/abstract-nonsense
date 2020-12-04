package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Diagram {

    protected final Session session;
    private final Diagram parent;
    private final List<Diagram> children;

    public final List<Integer> indices;
    public final Map<Representation, Morphism> representations;
    private final Map<String, Morphism> symbols;

    public Diagram(Session session, Diagram parent) {
        this.session = (session != null ? session : (Session) this);
        this.parent = parent;

        indices = new ArrayList<>();
        symbols = new LinkedHashMap<>();
        representations = new LinkedHashMap<>();

        if (hasParent())
            parent.children.add(this);
        children = new ArrayList<>();
    }

    public void detach() {
        if (hasParent())
            parent.children.remove(this);
    }

    public boolean hasParent() {
        return parent != null;
    }

    public Diagram getParent() {
        return parent;
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

    public List<Representation> getRepresentations(Morphism x) {
        List<Representation> reps = new ArrayList<>();
        for (Map.Entry<Representation, Morphism> entry : representations.entrySet()) {
            if (entry.getValue().equals(x))
                reps.add(entry.getKey());
        }

        if (hasParent() && !owns(x))
            reps.addAll(parent.getRepresentations(x));

        return reps;
    }

    public boolean owns(Morphism f) {
        return indices.contains(f.index);
    }

    public boolean owns(int index) {
        return indices.contains(index);
    }

    private boolean ownsAny(List<Morphism> list) {
        for (Morphism x : list) {
            if (owns(x))
                return true;
        }
        return false;
    }

    boolean knows(Morphism x) {
        if (owns(x))
            return true;
        if (hasParent())
            return parent.knows(x);
        return false;
    }

    // --- Creation methods ---

//    public Number createNumber(int n) {
//        // Numbers should exist in the bottom diagrams: those which have no parent
//        if (hasParent())
//            return parent.createNumber(n);
//
//        // If the number was already created, just return that one
//        String strN = String.valueOf(n);
//
//        if (hasSymbol(strN))
//            return (Number) getMorphism(strN);
//
//        // Construct a new number with symbolic representation
//        Number N = new Number(n);
//        addMorphism(N);
//        assignSymbol(strN, N);
//        return N;
//    }

    public Morphism createComposition(List<Morphism> list) throws CreationException {
        // If this diagram does not own any of the morphisms, refer to parent
        if (hasParent() && !ownsAny(list))
            return parent.createComposition(list);

        // There must be at least one morphism
        if (list.isEmpty())
            throw new CreationException("There must be at least one morphism!");

        // Obtain (co)domain
        int n = list.size();
        Morphism X = session.dom(list.get(list.size() - 1));
        Morphism Y = session.cod(list.get(0));

        // The morphisms must connect (this automatically makes sure all morphisms are in the same category, and that the k-values are okay)
        for (int i = 0; i < n - 1; ++i) {
            if (!session.dom(list.get(i)).equals(session.cod(list.get(i + 1))))
                throw new CreationException("Morphisms do not connect well!");
        }

        // We can simply remove all identity morphisms
        list.removeIf(session::isIdentity);

        // If the list is empty now, then the result would have been id_X = id_Y
        n = list.size();
        if (n == 0)
            return session.id(X);

        // If there is only one morphism left, just return that morphism
        if (n == 1)
            return list.get(0);

        // Create representation
        Representation rep = Representation.createComposition(list);
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Create morphism
        Morphism g = session.createMorphism(this, session.cat(list.get(0)), list.get(0).k, X, Y);
        representations.put(rep, g);
        return g;
    }

    public Morphism createPropertyApplication(Property property, List<Morphism> data) throws CreationException {
        // Check if parent should create this instead
        if (hasParent() && !ownsAny(data))
            return parent.createPropertyApplication(property, data);

        // The data should fit in the context of the property
        Mapping mapping = property.context.mappingFromData(this, data);
        if(mapping == null || !mapping.valid())
            throw new CreationException("Property does not apply to the given data");

        // Create representation
        Representation rep = Representation.createProperty(property, data);
        if (representations.containsKey(rep))
            return representations.get(rep);

        // If property has a definition, use that one. Otherwise, create proposition
        Morphism P;
        if(property.definition != null)
            P = mapping.map(property.definition);
        else
            P = session.createObject(this, session.Prop);
        representations.put(rep, P);
        return P;
    }

    public Morphism createFunctorApplication(Morphism F, Morphism x) throws CreationException {
        // TODO: I don't quite know what the general structure would be, so for now just do this case-wise

        // Check if parent should create this instead
        if (hasParent() && !owns(F) && !owns(x))
            return parent.createFunctorApplication(F, x);

        // Check if the representation already exists in the diagram, and if so, return the morphism it points to
        Representation rep = Representation.createFunctor(F, x);
        if (representations.containsKey(rep))
            return representations.get(rep);

        Morphism Fx = null;

        // If F is a 1-morphism in Set, it maps elements between sets
        if(session.cat(F).equals(session.Set) && F.k == 1) {
            // x must be a 0-morphism in 1-dom(F)
            if(!session.cat(x).equals(session.dom(F)) || x.k != 0)
                throw new CreationException("Function not applicable to given input");

            // If F is the identity map, just return x
            if(session.isIdentity(F))
                return x;

            // Otherwise, create an element in cod(F)
            Fx = session.createObject(this, session.cod(F));
        }

        // If F is a 1-morphism in Cat, it maps objects to objects, and morphisms to morphisms
        if(session.cat(F).equals(session.Cat) && F.k == 1) {
            // x must be a 0-morphism or 1-morphism in 1-dom(F)
            if((x.k != 0 && x.k != 1) || !session.cat(x).equals(session.dom(F)))
                throw new CreationException("Functor not applicable to given input");

            // If F is the identity map, just return x
            if(session.isIdentity(F))
                return x;

            // If x is a 0-morphism, F(x) is a 0-morphism in 1-cod(F)
            if(x.k == 0)
                Fx = session.createObject(this, session.cod(F));

            // If x is a 1-morphism a -> b, F(x) is a 1-morphism F(a) -> F(b) in 1-cod(F)
            if(x.k == 1) {
                Morphism X = session.dom(x), Y = session.cod(x);
                Morphism FX = createFunctorApplication(F, X);
                Morphism FY = createFunctorApplication(F, Y);
                Fx = session.createMorphism(this, session.cod(F), 1, FX, FY);
            }
        }

        if(Fx == null)
            throw new CreationException("Don't know about this kind of functor construction yet?");

        // Assign, and add morphism and representation to the diagram
        representations.put(rep, Fx);
        return Fx;
    }

    public Morphism createAnd(Morphism P, Morphism Q) throws CreationException {
        // Check if parent should create this instead
        if (hasParent() && !owns(P) && !owns(Q))
            return parent.createAnd(P, Q);

        // P and Q must be Prop's
        if(!session.cat(P).equals(session.Prop) || !session.cat(Q).equals(session.Prop))
            throw new CreationException("'&' only applies to Propositions");

        // Trivialities
        if(P.equals(session.False) || Q.equals(session.False))
            return session.False;
        if(P.equals(session.True))
            return Q;
        if(Q.equals(session.True))
            return P;

        // Create representation
        Representation rep = Representation.createAnd(P, Q);
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Create proposition
        Morphism R = session.createObject(this, session.Prop);
        representations.put(rep, R);
        return R;
    }

    public Morphism createOr(Morphism P, Morphism Q) throws CreationException {
        // Check if parent should create this instead
        if (hasParent() && !owns(P) && !owns(Q))
            return parent.createOr(P, Q);

        // P and Q must be Prop's
        if(!session.cat(P).equals(session.Prop) || !session.cat(Q).equals(session.Prop))
            throw new CreationException("'|' only applies to Propositions");

        // Trivialities
        if(P.equals(session.True) || Q.equals(session.True))
            return session.True;
        if(P.equals(session.False))
            return Q;
        if(Q.equals(session.False))
            return P;

        // Create representation
        Representation rep = Representation.createOr(P, Q);
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Create proposition
        Morphism R = session.createObject(this, session.Prop);
        representations.put(rep, R);
        return R;
    }

    public Morphism createHom(Morphism f, Morphism g) throws CreationException {
        // Check if parent should create this instead
        if (hasParent() && !owns(f) && !owns(g))
            return parent.createHom(f, g);

        // f and g must be comparable
        if(!session.comparable(f, g))
            throw new CreationException("given morphisms are not comparable");

        // Special cases
        if(session.cat(f).equals(session.Prop)) {
            if(f.equals(session.True))
                return g;
            if(g.equals(session.True))
                return session.True;
            if(g.equals(f))
                return session.True;
        }

        // Check if the representation already exists in the diagram, and if so, return the morphism it points to
        Representation rep = Representation.createHom(f, g);
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Create morphism
        int n = session.degree(f);
        Morphism h = session.createObject(this, session.nCat(session.degree(f)));
        representations.put(rep, h);
        if(n >= 0)
            session.setHom(h.index, f, g); // Sort of workaround, don't know how to do it cleaner

        // If g = False, we are talking about a negation: automatically set ~~P = P
        if(g.equals(session.False)) {
            Representation rep_neg = Representation.createHom(h, session.False);
            if(!representations.containsKey(rep_neg))
                representations.put(rep_neg, f);
            else
                session.identify(representations.get(rep_neg), f); // TODO: this might fail..
        }

        return h;
    }

    public Morphism createEquality(Morphism f, Morphism g) throws CreationException {
        // Check if parent should create this instead
        if (hasParent() && !owns(f) && !owns(g))
            return parent.createEquality(f, g);

        // Check if the representation already exists in the diagram, and if so, return the morphism it points to
        Representation rep = Representation.createEquality(f, g);
        if (representations.containsKey(rep))
            return representations.get(rep);

        // f and g must have same domain and codomain in the same category
        if(!session.comparable(f, g))
            throw new CreationException("Given morphisms are incomparable");

        // Special case
        if(f.equals(g))
            return session.True;

        // Create proposition
        Morphism P = session.createObject(this, session.Prop);
        representations.put(rep, P);
        return P;
    }

    private Morphism createFromRepresentation(Representation rep) throws CreationException {
        return createFromRepresentation(rep, null);
    }

    public Morphism createFromRepresentation(Representation rep, Mapping mapping) throws CreationException {
        if(mapping != null) {
            return switch (rep.type) {
                case HOM -> createHom(mapping.map(rep.data.get(0)), mapping.map(rep.data.get(1)));
                case EQUALITY -> createEquality(mapping.map(rep.data.get(0)), mapping.map(rep.data.get(1)));
                case AND -> createAnd(mapping.map(rep.data.get(0)), mapping.map(rep.data.get(1)));
                case OR -> createOr(mapping.map(rep.data.get(0)), mapping.map(rep.data.get(1)));
                case COMPOSITION -> createComposition(mapping.map(rep.data));
                case FUNCTOR_APPLICATION -> createFunctorApplication(mapping.map(rep.data.get(0)), mapping.map(rep.data.get(1)));
                case PROPERTY_APPLICATION -> createPropertyApplication(rep.property, mapping.map(rep.data));
            };
        }
        else {
            return switch (rep.type) {
                case HOM -> createHom(rep.data.get(0), rep.data.get(1));
                case EQUALITY -> createEquality(rep.data.get(0), rep.data.get(1));
                case AND -> createAnd(rep.data.get(0), rep.data.get(1));
                case OR -> createOr(rep.data.get(0), rep.data.get(1));
                case COMPOSITION -> createComposition(rep.data);
                case FUNCTOR_APPLICATION -> createFunctorApplication(rep.data.get(0), rep.data.get(1));
                case PROPERTY_APPLICATION -> createPropertyApplication(rep.property, rep.data);
            };
        }
    }
//
//    public void resolveEqualities() throws CreationException {
//        Set<Map.Entry<Representation, Morphism>> entrySet = new HashSet<>(representations.entrySet());
//        for (Map.Entry<Representation, Morphism> entry : entrySet) {
//            Representation rep = entry.getKey();
//            if (rep.property != Global.Equals)
//                continue;
//
//            if (knowsInstance(entry.getValue()))
//                setEqual(rep.data.get(0), rep.data.get(1));
//        }
//    }
//

    protected void replaceMorphism(Morphism f, Morphism g, List<MorphismPair> induced) throws CreationException {
        // Replace symbol pointers
        for (Map.Entry<String, Morphism> entry : symbols.entrySet()) {
            Morphism h = entry.getValue();
            if(h.index == f.index)
                entry.setValue(new Morphism(g.index, h.k));
        }

        // Replace representation pointers
        for (Map.Entry<Representation, Morphism> entry : representations.entrySet()) {
            Morphism h = entry.getValue();
            if(h.index == f.index)
                entry.setValue(new Morphism(g.index, h.k));
        }

        // If a representation contains f as data, recreate it
        for (Map.Entry<Representation, Morphism> entry : new HashSet<>(representations.entrySet())) {
            Representation rep = entry.getKey();

            boolean flag = false; // TODO: Can this be shortened?
            for(Morphism h : rep.data) {
                if(h.index == f.index) {
                    flag = true;
                    break;
                }
            }
            if(!flag)
                continue;

            representations.remove(rep);
            rep.data.replaceAll(h -> (h.equals(f) ? g : h));
            Morphism h = createFromRepresentation(rep);
            induced.add(new MorphismPair(entry.getValue(), h));
        }

        // Also make replacements in children
        for (Diagram child : children)
            child.replaceMorphism(f, g, induced);
    }

    // ------------ Stringify ------------

    public String strList(List<Morphism> list) {
        return strList(list, ", ");
    }

    public String strList(List<Morphism> list, String delimiter) {
        if(list.isEmpty())
            return "";
        StringJoiner sj = new StringJoiner(delimiter);
        for (Morphism x : list)
            sj.add(str(x));
        return sj.toString();
    }

    public String str(Morphism x) {
        if(session.isIdentity(x))
            return "id(" + str(new Morphism(x.index, x.k - 1)) + ")";

        for (Map.Entry<String, Morphism> entry : symbols.entrySet()) {
            if (entry.getValue().equals(x))
                return entry.getKey();
        }

        for (Map.Entry<Representation, Morphism> entry : representations.entrySet()) {
            if (entry.getValue().equals(x)) {
                Representation rep = entry.getKey();
                return switch (rep.type) {
                    case HOM -> rep.data.get(1).equals(session.False) ? "~" + str(rep.data.get(0)) : (str(rep.data.get(0)) + " -> " + str(rep.data.get(1)));
                    case EQUALITY -> str(rep.data.get(0)) + " = " + str(rep.data.get(1));
                    case AND -> str(rep.data.get(0)) + " & " + str(rep.data.get(1));
                    case OR -> str(rep.data.get(0)) + " | " + str(rep.data.get(1));
                    case COMPOSITION -> strList(rep.data, ".");
                    case FUNCTOR_APPLICATION -> str(rep.data.get(0)) + "(" + str(rep.data.get(1)) + ")";
                    case PROPERTY_APPLICATION -> rep.property.name + "(" + strList(rep.data) + ")";
                };
            }
        }

        if (hasParent() && !owns(x))
            return parent.str(x);

        return "[" + x.index + "]";
    }

    protected void addIndex(int index) {
        indices.add(index);
    }

    protected void removeIndex(Integer index) {
        indices.remove(index);
    }

}
