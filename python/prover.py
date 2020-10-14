#!/usr/bin/env python
# coding: utf-8

# In[1]:


from core import *
from mapper import *


# In[2]:


class Prover:

    # Tries to find a proof in diagram of C, i.e. an instance of the category C
    
    def __init__(self, diagram, C):
        self.diagram = diagram
        self.C = C
        
        # Already store the representations of C for later use
        self.reps_C = diagram.get_representations(C)
        
    def prove(self):
        # If there is already an instance of C, we are done
        if self.diagram.has_instance_of(self.C):
            return True
        
        # Find an applicable theorem
        if isinstance(self.diagram, Book):
            book = self.diagram
            for thm_name in book.theorems:
                thm = book.theorems[thm_name]
                options = self.theorem_applicable(thm)
                
                print('For theorem {}, options are {}'.format(thm_name, options))
                
                for option in options:
                    M = Mapper(thm, self.diagram)
                    M.mapping_set_multiple(option)
                    if M.find_mapping():
                        if thm.try_application(self.diagram, M.mapping):
                            print('Proved {} by applying Theorem {} to {}'.format(self.diagram.str_x(self.C), thm_name, M.mapping))
                            return True
        
        
        print('Was not able to prove {}'.format(self.diagram.str_x(self.C)))
        return False
    
    def theorem_applicable(self, thm):
        options = []
        
        # See if C can match some conclusion of the theorem
        for x in thm.conclusion.morphisms:
            # QUESTION: IS THERE ANY WAY THAT x.category WILL EQUAL self.C WHEN APPLYING THE THEOREM ??
            D = x.category
            
            # IF x.category == self.C ALREADY, THEN YES (with no conditions)
            if D == self.C:
                options.append({})
                continue
                
            # IF x.category IS OWNED BY THE CONCLUSION (I.E. IT WILL DEPEND ON SOMETHING 'THAT EXISTS'), NO MAPPING WILL BE POSSIBLE
            if thm.conclusion.owns(D):
                print('This is the problem..')
                continue
            
            # IF x.category DOES NOT BELONG TO THE THEOREM (CONTEXT), THEN NO (otherwise x.category should equal self.C already)
            if not thm.owns(D):
                continue            
            
            # WELL, IF x.category IS DATA, THEN YES (with condition x.category -> self.C)
            if D in thm.data:
                options.append({D : self.C})
                continue
            
            # AT THIS POINT, IF AND ONLY IF SOME REPRESENTATION OF x.category INDUCES self.C
            reps_D = thm.get_representations(D)            
            for r_D in reps_D:
                for r_C in self.reps_C:
                    option = self.reps_match_consequences(thm, r_D, r_C)
                    if option != None:
                        options.append(option)

        return options
    
    # Returns what the consequence is if r_x matches r_y
    def reps_match_consequences(self, context, r_x, r_y):
        if type(r_x) != type(r_y):
            return None
        
        if isinstance(r_x, Repr_Property):
            if r_x.prop != r_y.prop:
                return False

        m = {}
        r_x_deps = r_x.dependencies()
        r_y_deps = r_y.dependencies()
        
        for u, v in zip(r_x_deps, r_y_deps):
            if context.owns(u):
                m[u] = v
                
        return m


# In[ ]:





# In[ ]:




