package nl.jessetvogel.abstractnonsense.core;

import java.util.*;

public class Session extends Diagram {

    private int indexCounter = 0;
    private int indexTopCat = -1;

    private final Map<String, Property> properties;
    private final Map<String, Theorem> theorems;

    private final Map<Integer, Diagram> owner;
    private final Map<Integer, MorphismInfo> morphismInfo;

    public final List<Integer> nCat;
    public final Morphism True, False, Prop, Set, Cat;

    private final Map<Integer, MorphismPair> homs;

    public Session() {
        super(null, null);

        // Create maps
        properties = new HashMap<>();
        theorems = new HashMap<>();
        owner = new HashMap<>();
        morphismInfo = new HashMap<>();
        homs = new HashMap<>();

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
            e.printStackTrace();
        }
        False = F;

        // Define some symbols
        assignSymbol("Cat", Cat);
        assignSymbol("Set", Set);
        assignSymbol("True", True);
        assignSymbol("Prop", Prop);
        assignSymbol("False", False);
    }

    private Morphism newTopCat() {
        // C denotes the category of n-categories with n the currently highest balbalbal
        Morphism C = new Morphism(indexCounter++, 0);
        owner.put(C.index, this);
        addIndex(C.index);
        nCat.add(C.index);
        if(indexTopCat != -1)
            morphismInfo.put(indexTopCat, new MorphismInfo(C.index, 0, C.index, C.index));
        indexTopCat = C.index;
        return C;
    }

    public int degree(Morphism x) {
        return degree(x.index) - x.k;
    }

    public int degree(Integer index) {
        int m = nCat.indexOf(index);
        if(m != -1)
            return m - 1;
        MorphismInfo info = morphismInfo.get(index);
        return degree(info.cat) - info.k - 1;
    }

    private void morphismSetInfo(Morphism f) {
        if(f.info != null)
            return;
        if (f.index == indexTopCat)
            newTopCat();
        f.info = morphismInfo.get(f.index);
    }

    public Morphism cat(Morphism f) {
        morphismSetInfo(f);
        return new Morphism(f.info.cat, 0);
    }

    public Morphism dom(Morphism f) {
        morphismSetInfo(f);
        if(f.k == f.info.k)
            return new Morphism(f.info.dom, f.k - 1);
        else
            return new Morphism(f.index, f.k - 1);
    }

    public Morphism cod(Morphism f) {
        morphismSetInfo(f);
        if(f.k == f.info.k)
            return new Morphism(f.info.cod, f.k - 1);
        else
            return new Morphism(f.index, f.k - 1);
    }

    public Morphism id(Morphism f) throws CreationException {
        morphismSetInfo(f);
        int n = degree(f.info.cat);
        if(f.k >= n)
            throw new CreationException("Cannot create " + (f.k + 1) + "-morphisms in a " + n + "-category");

        return new Morphism(f.index, f.k + 1);
    }

    public void setHom(int index, Morphism X, Morphism Y) {
        homs.put(index, new MorphismPair(X, Y));
    }

    public Morphism createObject(Diagram diagram, Morphism category) throws CreationException {
        // If given category is a hom-category, then create k-morphism for higher k
        if(homs.containsKey(category.index)) {
            MorphismPair pair = homs.get(category.index);
            return createMorphism(diagram, session.cat(pair.x), pair.x.k + 1, pair.x, pair.y);
        }

        return createMorphism(diagram, category, 0, category, category);
    }

    public Morphism createMorphism(Diagram diagram, Morphism category, int k, Morphism domain, Morphism codomain) throws CreationException {
        if(!isCategory(category))
            throw new CreationException("is not a category");
        // TODO: check: 'category' must be a category!

        // k must be non-negative
        if(k < 0)
            throw new CreationException("k must be non-negative");

        // If k > 0, k must be at most X.k() + 1 and Y.k() + 1
        if(k > 0 && (domain.k != k - 1 || codomain.k != k - 1))
            throw new CreationException("k-morphisms must be between (k-1)-morphisms");

        // If k > 0, category must agree with the categories of domain and codomain
        if(k > 0 && (!cat(domain).equals(category) || !cat(codomain).equals(category)))
            throw new CreationException("domain or codomain does not lie in the specified category");

        // If k > 1, the domain and codomain of the domain and codomain must agree
        if(k > 1 && (!dom(domain).equals(dom(codomain)) || !cod(domain).equals(cod(codomain))))
            throw new CreationException("domain and codomain or domain and codomain must agree");

        // k must be at most the n-value of C
        int n = session.degree(category);
        if(k > n)
            throw new CreationException("cannot create a " + k + "-morphism in a " + n + "-category");

        // If all is verified, allocate a new index for this morphism and set info
        int index = indexCounter++;
        morphismInfo.put(index, new MorphismInfo(category.index, k, domain.index, codomain.index));
        owner.put(index, diagram);
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

    public Morphism nCat(int n) {
        int i = n + 2; // Mind the offset
        while(i >= nCat.size())
            newTopCat();
        return new Morphism(nCat.get(i), 0);
    }

    public boolean isIdentity(Morphism f) {
        morphismSetInfo(f);
        return f.k > f.info.k;
    }

//    public Morphism liftIdentity(Morphism x) {
//        return new Morphism(x.index, x.k - 1);
//    }

    public boolean isCategory(Morphism C) {
        return nCat.contains(cat(C).index);
    }

    // Identifies x with y (that is, y remains)
    public void identify(Morphism x, Morphism y) throws Exception {
        // If x and y are already equal, we are done
        if (x.equals(y))
            return;

        // x and y must be comparable
        if(!comparable(x, y))
            throw new Exception("incomparable morphisms");

        // TODO: must we 'descend' y if necsesary?

//        // Idea is to replace x with y, so make sure that this diagram owns x
//        if (!owns(x)) {
//            Morphism z = y;
//            y = x;
//            x = z;
//        }

        // Make changes in MorphismInfo
        for(MorphismInfo info : morphismInfo.values()) {
            if(info.cat == x.index) info.cat = y.index;
            if(info.dom == x.index) info.dom = y.index;
            if(info.cod == x.index) info.cod = y.index;
        }

        // Now we replace x with y (keeping track of induced identifications) starting from the Diagram that owns x (any Diagram below that won't know of x)
        List<MorphismPair> induced = new ArrayList<>();
        owner(x).replaceMorphism(x, y, induced);

        // Delete index from diagram and remove owner
        owner(x).removeIndex(x.index);
        owner.remove(x.index);
        morphismInfo.remove(x.index);

        // TODO: anything to do afterwards?



        // Continue with possible induced equalities
        for (MorphismPair pair : induced)
            identify(pair.x, pair.y);
    }

    public boolean comparable(Morphism x, Morphism y) {
        if(x.k != y.k)
            return false;
        if(!cat(x).equals(cat(y)))
            return false;
        if(x.k > 0 && (!dom(x).equals(dom(y)) || !cod(x).equals(cod(y))))
            return false;
        return true;
    }

    public Diagram owner(Morphism x) {
        return owner.get(x.index);
    }

    public Morphism morphism(int index) {
        MorphismInfo info = morphismInfo.get(index);
        if(info == null)
            return null;

        Morphism f = new Morphism(index, info.k);
        f.info = info;
        return f;
    }

    public String signature(List<Morphism> list) {
        StringJoiner sj = new StringJoiner(",", "(", ")");
        for(Morphism x : list)
            sj.add(String.valueOf(x.k));
        return sj.toString();
    }
}
