package nl.jessetvogel.abstractnonsense.prover;

import nl.jessetvogel.abstractnonsense.core.*;

import java.util.ArrayList;

public class Prover {

    Diagram diagram;
    Morphism P;

    ArrayList<Representation> repsP;

    public Prover(Diagram diagram, Morphism P) {
        this.diagram = diagram;
        this.P = P;
        this.repsP = diagram.getRepresentations(P);
    }

    public boolean prove() {
        // If there already is an instance of P, we are done
        if(diagram.knowsInstance(P))
            return true;

        // Find applicable theorems
        Book book = (Book) diagram;
        for(Theorem thm : book.getTheorems()) {
            Diagram conclusion = thm.getConclusion();
            for(Morphism x : conclusion.morphisms) {
                // x MUST BE AN OBJECT
                if(!x.isObject())
                    continue;

                //QUESTION: IS THERE ANY WAY THAT x.category WILL EQUAL P WHEN APPLYING THE THEOREM ??
                Morphism C = x.category;

                // IF x.category == P ALREADY, THEN YES (with no conditions)
                if(C == P) {
                    Mapping mapping = new Mapping(thm, diagram);
                    if(thm.tryApplication(mapping))
                        return true;
                    continue;
                }

                // IF x.category IS OWNED BY THE CONCLUSION (I.E. IT WILL DEPEND ON SOMETHING 'THAT EXISTS'), NO MAPPING WILL BE POSSIBLE
                if(conclusion.owns(C))
                    continue;

                // IF x.category DOES NOT BELONG TO THE THEOREM (CONTEXT), THEN NO (otherwise x.category should equal P already)
                if(!thm.owns(C))
                    continue;

                // WELL, IF x.category IS DATA, THEN YES (with condition x.category -> P)
                if(thm.isData(C)) {
                    Mapping mapping = new Mapping(thm, diagram);
                    if(!mapping.set(C, P))
                        return false;
                    if(thm.tryApplication(mapping))
                        return true;
                    continue;
                }

                // AT THIS POINT, IF AND ONLY IF SOME REPRESENTATION OF x.category INDUCES P
                ArrayList<Representation> repsC = thm.getRepresentations(C);
                for(Representation rC : repsC) {
                    for(Representation rP : repsP) {
                        Mapping mapping = mappingFromRepresentations(thm, rC, rP);
                        if(mapping != null && thm.tryApplication(mapping))
                            return true;
                    }
                }
            }
        }

        return false;
    }

    private Mapping mappingFromRepresentations(Context context, Representation r, Representation s) {
        if(r.type != s.type)
            return null;
        if(r.type == Representation.Type.PROPERTY_APPLICATION && r.property != s.property)
            return null;

        Mapping mapping = new Mapping(context, diagram);
        int n = r.data.size();
        for(int i = 0; i < n; ++i) {
            Morphism x = r.data.get(i);
            if(context.owns(x) && !mapping.set(x, s.data.get(i)))
                return null;
        }

        return mapping;
    }
}
