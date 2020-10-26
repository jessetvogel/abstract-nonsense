package nl.jessetvogel.abstractnonsense.core;

import java.lang.reflect.Array;
import java.util.*;

import static nl.jessetvogel.abstractnonsense.core.Global.Cat;

public class Diagram {

    public ArrayList<Morphism> morphisms;
    Map<String, Morphism> symbols;
    Map<Representation, Morphism> representations;

    Diagram parent;
    ArrayList<Diagram> children;

    Diagram(Diagram parent) {
        morphisms = new ArrayList<>();
        symbols = new HashMap<>();
        representations = new LinkedHashMap<>();
        children = new ArrayList<>();

        this.parent = parent;
        if (hasParent())
            parent.children.add(this);
    }

    boolean hasParent() {
        return parent != null;
    }

    public void addMorphism(Morphism x) {
        morphisms.add(x);
    }

    void assignRepresentation(Representation rep, Morphism x) {
        representations.put(rep, x);
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

    public Property getProperty(String name) {
        if (hasParent())
            return parent.getProperty(name);
        return null;
    }

    public Theorem getTheorem(String name) {
        if (hasParent())
            return parent.getTheorem(name);
        return null;
    }

    public ArrayList<Representation> getRepresentations(Morphism x) {
        ArrayList<Representation> reps = new ArrayList<>();
        for (Map.Entry<Representation, Morphism> entry : representations.entrySet()) {
            if (entry.getValue() == x)
                reps.add(entry.getKey());
        }

        if (hasParent() && !owns(x))
            reps.addAll(parent.getRepresentations(x));

        return reps;
    }

    public boolean knowsInstance(Morphism C) {
        // A number n has an instance precisely if n > 0
        if (C instanceof Number && ((Number) C).n > 0)
            return true;

        // Check if it has an instance itself
        for (Morphism x : morphisms) {
            if (x.category == C)
                return true;
        }

        // If it does not own C, ask its parent
        if (hasParent() && !owns(C))
            return parent.knowsInstance(C);

        return false;
    }

    public boolean owns(Morphism x) {
        return morphisms.contains(x);
    }

    private boolean ownsSomething(ArrayList<Morphism> list) {
        for (Morphism x : list) {
            if (owns(x))
                return true;
        }
        return false;
    }

    boolean owns(Property property) {
        return false;
    }

    boolean knows(Morphism x) {
        if (owns(x))
            return true;
        if (hasParent())
            return parent.knows(x);
        return false;
    }

    Diagram getOwner(Morphism x) {
        if (owns(x))
            return this;
        if (hasParent())
            return parent.getOwner(x);
        return null;
    }

    // --- Creation methods ---

    public Morphism createObject(Morphism C) {
        // C must be a category
        if (C.category != Cat) {
            System.out.println("Must be a category!");
            return null;
        }

        // Construct new object
        Morphism X = new Morphism(C);
        addMorphism(X);
        return X;
    }

    public Morphism createMorphism(Morphism X, Morphism Y, boolean covariant) {
        // The categories of X and Y must be equal
        if (X.category != Y.category) {
            System.out.println("Cannot construct a morphism between objects of different categories!");
            return null;
        }

        // Construct new morphism
        Morphism f = new Morphism(X.category, X, Y);
        if (X.category == Cat)
            f.covariant = covariant;
        addMorphism(f);
        return f;
    }

    public Number createNumber(int n) {
        // Numbers should exist in the bottom diagrams: those which have no parent
        if (hasParent())
            return parent.createNumber(n);

        // If the number was already created, just return that one
        String strN = String.valueOf(n);

        if (hasSymbol(strN))
            return (Number) getMorphism(strN);

        // Construct a new number with symbolic representation
        Number N = new Number(n);
        addMorphism(N);
        assignSymbol(strN, N);
        return N;
    }

    public Morphism createComposition(ArrayList<Morphism> fList) {
        // Check if parent should create this instead
        if (hasParent() && !ownsSomething(fList))
            return parent.createComposition(fList);

        // There must be at least one morphism
        if (fList.isEmpty()) {
            System.out.println("There must be morphisms here!");
            return null;
        }

        // Obtain (co)domain
        int n = fList.size();
        Morphism X = fList.get(fList.size() - 1).domain;
        Morphism Y = fList.get(0).codomain;

        // All morphisms must connect
        for (int i = 0; i < n; ++i) {
            if (fList.get(i).domain != fList.get(i + 1).codomain) {
                System.out.println("Morphisms do not connect well!");
                return null;
            }
        }

        // Remove all identity morphisms list
        fList.removeIf(Morphism::isObject);

        // If the list is empty now, then would have been id(X) = id(Y)
        n = fList.size();
        if (n == 0)
            return X;

        // If there is only one morphism to compose, just return that morphism
        if (n == 1)
            return fList.get(0);

        // Create representation
        Representation rep = new Representation(Representation.Type.COMPOSITION, fList);

        // Check if the representation already exists in the diagram, and if so, return the morphism it points to
        Morphism g = representations.get(rep);
        if (g != null)
            return g;

        // Construct new object/morphism
        g = new Morphism(X.category, X, Y);

        // If we are talking about functors, determine whether the resulting functor is covariant or contravariant
        if (g.isFunctor()) {
            for (Morphism f : fList)
                g.covariant ^= f.covariant;
        }

        // Assign, and add morphism and representation to the diagram
        addMorphism(g);
        assignRepresentation(rep, g);
        return g;
    }

    public Morphism createPropertyApplication(Property property, ArrayList<Morphism> data) throws CreationException {
        // Check if parent should create this instead
        if (hasParent() && !owns(property) && !ownsSomething(data))
            return parent.createPropertyApplication(property, data);

        // The data should fit in the context of the property
        if (!property.isValidData(this, data))
            throw new CreationException("Property does not apply to the given data");

        // Special cases:
        if (property == Global.And && data.get(0) instanceof Number && data.get(1) instanceof Number)
            return createNumber(((Number) data.get(0)).n * ((Number) data.get(1)).n);

        if (property == Global.Or && data.get(0) instanceof Number && data.get(1) instanceof Number)
            return createNumber(((Number) data.get(0)).n + ((Number) data.get(1)).n);

        if (property == Global.Implies && data.get(0) instanceof Number && data.get(1) instanceof Number)
            return createNumber((int) Math.pow(((Number) data.get(0)).n, ((Number) data.get(1)).n));

        if (property == Global.Equals && data.get(0) == data.get(1))
            return Global.True;

        if (property == Global.Equals && data.get(0) instanceof Number && data.get(1) instanceof Number)
            return ((Number) data.get(0)).n == ((Number) data.get(1)).n ? Global.True : Global.False;

        // Create representation
        Representation rep = new Representation(Representation.Type.PROPERTY_APPLICATION, property, data);

        // Check if this representation already exists somewhere!
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Construct new category
        Morphism C = new Morphism(Cat);

        // Assign, and add object and representation to the diagram
        addMorphism(C);
        assignRepresentation(rep, C);
        return C;
    }

    public Morphism createFunctorApplication(Morphism F, Morphism x) throws CreationException {
        // Check if parent should create this instead
        if (!owns(F) && !owns(x) && hasParent())
            return parent.createFunctorApplication(F, x);

        // F must be a functor
        if (!F.isFunctor())
            throw new CreationException("Given morphism is not a functor");

        // x must belong to the domain category of the functor
        if (x.category != F.domain)
            throw new CreationException("Given morphism does not belong to functor domain");

        // Create representation
        ArrayList<Morphism> data = new ArrayList<>();
        data.add(F);
        data.add(x);
        Representation rep = new Representation(Representation.Type.FUNCTOR_APPLICATION, data);

        // Check if the representation already exists in the diagram, and if so, return the morphism it points to
        if (representations.containsKey(rep))
            return representations.get(rep);

        // Construct new object/morphism
        Morphism Fx;
        if (x.isObject()) {
            Fx = new Morphism(F.codomain);
        } else {
            Morphism X = x.domain, Y = x.codomain;
            Morphism FX = createFunctorApplication(F, X);
            Morphism FY = createFunctorApplication(F, Y);
            if (F.covariant)
                Fx = new Morphism(F.codomain, FX, FY);
            else
                Fx = new Morphism(F.codomain, FY, FX);
        }

        // Assign, and add morphism and representation to the diagram
        addMorphism(Fx);
        assignRepresentation(rep, Fx);
        return Fx;
    }

    private Morphism createFromRepresentation(Representation rep) throws CreationException {
        return switch (rep.type) {
            case COMPOSITION -> createComposition(rep.data);
            case FUNCTOR_APPLICATION -> createFunctorApplication(rep.data.get(0), rep.data.get(1));
            case PROPERTY_APPLICATION -> createPropertyApplication(rep.property, rep.data);
        };
    }

    public Morphism createFromPlaceholders(Representation rep, Mapping mapping) throws CreationException {
        return switch (rep.type) {
            case COMPOSITION -> createComposition(mapping.mapList(rep.data));
            case FUNCTOR_APPLICATION -> createFunctorApplication(mapping.map(rep.data.get(0)), mapping.map(rep.data.get(1)));
            case PROPERTY_APPLICATION -> createPropertyApplication(rep.property, mapping.mapList(rep.data));
        };
    }

    public void resolveEqualities() throws CreationException {
        for (Map.Entry<Representation, Morphism> entry : representations.entrySet()) {
            Representation rep = entry.getKey();
            if (rep.property != Global.Equals)
                continue;

            if (knowsInstance(entry.getValue()))
                setEqual(rep.data.get(0), rep.data.get(1));
        }
    }

    public void setEqual(Morphism x, Morphism y) throws CreationException {
        // If do not own x nor y, then parent should set them equal
        if (!owns(x) && !owns(y)) {
            if (hasParent()) {
                parent.setEqual(x, y);
                return;
            }
            System.err.println("Oops, this should not be happening...");
        }

        // If x and y are already equal, there is nothing to do
        if (x == y)
            return;

        // If either x or y is not an object, first their domain and codomain must match
        if(!x.isObject() || !y.isObject()) {
            setEqual(x.domain, y.domain);
            setEqual(x.codomain, y.codomain);
        }

        // Idea is to replace x with y, so make sure that this diagram owns x
        if (!owns(x)) {
            Morphism z = y;
            y = x;
            x = z;
        }

        // Now we replace x with y
        List<InducedEquality> induced = new ArrayList<>();
        replaceMorphism(x, y, induced);

        // At this point, nothing references x anymore in this diagram or its children.

        // Delete x
        morphisms.remove(x);

        // Continue with possible induced equalities
        for (InducedEquality e : induced)
            e.diagram.setEqual(e.x, e.y);
    }

    private void replaceMorphism(Morphism x, Morphism y, List<InducedEquality> induced) throws CreationException {
        // Replace categories, domains and codomains
        for (Morphism z : morphisms) {
            if (z.category == x) z.category = y;
            if (z.domain == x) z.domain = y;
            if (z.codomain == x) z.codomain = y;
        }

        // Replace symbol pointers
        for (Map.Entry<String, Morphism> entry : symbols.entrySet()) {
            if (entry.getValue() == x)
                entry.setValue(y);
        }

        // Replace representation pointers
        for (Map.Entry<Representation, Morphism> entry : representations.entrySet()) {
            if (entry.getValue() == x)
                entry.setValue(y);
        }

        // Replace representations containing x as data
        for (Iterator<Map.Entry<Representation, Morphism>> it = representations.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Representation, Morphism> entry = it.next();
            Representation rep = entry.getKey();
            if (rep.data.contains(x)) {
                it.remove();
                rep.data.replaceAll(z -> (z == x ? y : z));
                Morphism z = createFromRepresentation(rep);
                induced.add(new InducedEquality(this, entry.getValue(), z));
            }
        }

        // Apply to children
        for (Diagram child : children)
            child.replaceMorphism(x, y, induced);
    }

    // ------------ Stringify ------------

    public String strList(ArrayList<Morphism> list) {
        StringBuilder s = new StringBuilder();
        for (Morphism x : list)
            s.append(str(x)).append(",");
        s.deleteCharAt(s.length() - 1);
        return s.toString();
    }

    public String str(Morphism x) {
        for (Map.Entry<String, Morphism> entry : symbols.entrySet()) {
            if (entry.getValue() == x)
                return entry.getKey();
        }

        for (Map.Entry<Representation, Morphism> entry : representations.entrySet()) {
            if (entry.getValue() == x) {
                Representation rep = entry.getKey();
                return switch (rep.type) {
                    case COMPOSITION -> strList(rep.data).replaceAll(",", ".");
                    case FUNCTOR_APPLICATION -> str(rep.data.get(0)) + "(" + str(rep.data.get(1)) + ")";
                    case PROPERTY_APPLICATION -> rep.property.name + "(" + strList(rep.data) + ")";
                };
            }
        }

        if (hasParent() && !owns(x))
            return parent.str(x);

        return "?";
    }

    private class InducedEquality {

        final Diagram diagram;
        final Morphism x, y;

        InducedEquality(Diagram diagram, Morphism x, Morphism y) {
            this.diagram = diagram;
            this.x = x;
            this.y = y;
        }
    }
}
