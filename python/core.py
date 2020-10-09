#!/usr/bin/env python
# coding: utf-8

# ### Notation:
# Since objects are considered morphisms (an object $X$ is identified with its identity morphism $\text{id}_X$), we write `X, Y, Z, ...` for objects, `f, g, h, ...` for morphisms that are not objects, and `x, y, z, ...` for morphisms for which itis unknown whether they are objects / identity morphisms.
# 
# Objects which are categories are denoted by `C, D, E, ...`.
# 
# Functors are denoted by `F, G, H, ...`

# In[1]:


class Morphism:
    
    def __init__(self, category, domain, codomain):
        self.category = category
        self.domain = domain
        self.codomain = codomain

    def is_object(self):
        return self == self.domain

    def is_functor(self):
        return self.category == Cat
    
    def is_category(self):
        return self.is_object() and self.is_functor()


# In[2]:


class Object(Morphism):

    def __init__(self, category = None):
        # Object() creates the category of categories (whose category is itself)
        if not category:
            category = self
        
        # An object is identified with its identity morphism
        super().__init__(category, self, self)


# In[3]:


# Idea is good, but very cumbersome to distinguish between case of Functor and case of just Morphism (same for Object and Category)

# class Functor(Morphism):
    
#     def __init__(self, category, domain, codomain, covariant = True):
#         self.covariant = covariant
#         super().__init__(category, domain, codomain)


# In[4]:


# class Category(Object, Functor):

#     def __init__(self, self_category = False):
#         if self_category:
#             super().__init__(self)
#         else:
#             super().__init__(Cat)


# In[5]:


class Number(Object):
    
    def __init__(self, n):
        self.n = n
        super().__init__(Cat)


# In[6]:


class Representation:
        
    def __init__(self):
        self.ptr = None
        
    def assign(self, ptr):
        self.ptr = ptr
            
class Repr_Symbol(Representation):
    
    def __init__(self, name):
        self.name = name
        
    def __eq__(self, other):
        return type(self) == type(other) and self.name == other.name
    
    def dependencies(self):
        if self.ptr.is_object():
            return [ self.ptr.category ]
        else:
            return [ self.ptr.domain, self.ptr.codomain ]
    
class Repr_Composition(Representation):
    
    def __init__(self, f_list):        
        self.f_list = f_list
    
    def __eq__(self, other):
        return type(self) == type(other) and self.f_list == other.f_list
    
    def dependencies(self):
        return self.f_list

class Repr_Functor(Representation):
    
    def __init__(self, F, x):
        self.F = F
        self.x = x
        
    def __eq__(self, other):
        return type(self) == type(other) and self.F == other.F and self.x == other.x

    def dependencies(self):
        return [ self.F, self.x ]
    
class Repr_Property(Representation):
    
    def __init__(self, prop, data):
        self.prop = prop
        self.data = data
        
    def __eq__(self, other):
        return type(self) == type(other) and self.prop == other.prop and self.data == other.data
    
    def dependencies(self):
        return self.data
    
class Repr_Equality(Representation):
    
    def __init__(self, x, y):
        self.x = x
        self.y = y
    
    def __eq__(self, other):
        return type(self) == type(other) and ((self.x, self.y) == (other.x, other.y) or (self.x, self.y) == (other.y, other.x))
    
    def dependencies(self):
        return [ self.x, self.y ]

# class Repr_And(Representation):
    
#     def __init__(self, P, Q):
#         self.P = P
#         self.Q = Q
        
#     def __eq__(self, other):
#         return self.P == other.P and self.Q == other.Q
        
#     def depends_on(self, x):
#         return x == self.P or x == self.Q or super().depends_on(x)
        
# class Repr_Or(Representation):
    
#     def __init__(self, P, Q):
#         self.P = P
#         self.Q = Q
        
#     def __eq__(self, other):
#         return self.P == other.P and self.Q == other.Q
    
#     def depends_on(self, x):
#         return x == self.P or x == self.Q or super().depends_on(x)
        
# class Repr_Implies(Representation):
    
#     def __init__(self, P, Q):
#         self.P = P
#         self.Q = Q
        
#     def __eq__(self, other):
#         return self.P == other.P and self.Q == other.Q
    
#     def depends_on(self, x):
#         return x == self.P or x == self.Q or super().depends_on(x)
        
# class Repr_Not(Representation):
    
#     def __init__(self, P):
#         self.P = P
        
#     def __eq__(self, other):
#         return self.P == other.P
        
#     def depends_on(self, x):
#         return x == self.P or super().depends_on(x)


# In[7]:


def are_comparable(x, y):
    # Must lie in the same category
    if x.category != y.category:
        return False
    
    # If they are functors, their 'covariantness' must match
    if x.category.category == Cat and x.covariant != y.covariant:
        return False
    
    # Two morphisms are only comparable if their (co)domain matches, or if both are identity morphisms
    if (x.domain, x.codomain) != (y.domain, y.codomain) and not x.is_object() and y.is_object():
        return False
    
    return True


# In[8]:


class Diagram:
    
    def __init__(self):
        self.morphisms = []
        self.representations = []
        
        self.references = []
        self.citations = []
    
    def __del__(self):
        for ref in self.references:
            ref.citations.remove(self)
    
    def add_morphism(self, x):
        self.morphisms.append(x)
    
    def add_representation(self, rep):
        self.representations.append(rep)
    
    def add_reference(self, other):
        self.references.append(other)
        other.citations.append(self)
    
    def find_representation(self, rep):
        for r in self.representations:
            if r == rep:
                return r.ptr
                
        return None
    
    def find_morphism(self, name):
        for r in self.representations:
            if isinstance(r, Repr_Symbol) and r.name == name:
                return r.ptr
        
        for ref in self.references:
            x = ref.find_morphism(name)
            if x:
                return x
        
        return None
    
    def find_property(self, name):        
        for ref in self.references:
            p = ref.find_property(name)
            if p:
                return p
        
        return None
    
    def has_instance_of(self, C):
        for x in self.morphisms:
            if x.category == C:
                return True
        
        if not self.owns(C): # TODO: this is not clean! Not every reference even needs to know about C!    
            for ref in self.references:
                if ref.has_instance_of(C):
                    return True
            
        return False
    
    def is_name_available(self, name):
        if self.has_symbol(name):
            return False
        
        return True
    
    def has_symbol(self, name):
        for r in self.representations:
            if isinstance(r, Repr_Symbol) and r.name == name:
                return True
            
        return False
    
    def knows(self, x):
        if x in self.morphisms:
            return True
        
        for ref in self.references:
            if ref.knows(x):
                return True
        
        return False
    
    def owns(self, x):
        return x in self.morphisms
    
    def owner(self, x):
        if x in self.morphisms:
            return self
        
        if self.parent:
            return self.parent.owner(x)
        
        return None
    
    def create_name_like(self, name):
        used_names = [ r.name for r in self.representations if isinstance(r, Repr_Symbol) and r.name.startswith(name) ]
        
        while True:
            if name not in used_names:
                return name
            name = name + '\''
            
    def clean(self):
        # Remove all representations and morphisms that are unnecessary (whatever that means)
        # TODO: should this not be a method of Theorem?
        pass
    
#     def set_equal(self, x, y):
#         if x == y:
#             return
            
#         if x not in self.morphisms or y not in self.morphisms:
#             raise Exception('One of the two objects/morphisms does not belong to this diagram')
            
#         if not are_comparable(x, y):
#             raise Exception('These two objects/morphisms are not comparable!')
            
#         maak dit af
    
    # --- Factory methods ---
    
    def create_object(self, C, name = ''):        
        # C must be a category
        if C.category != Cat:
            raise Exception('That is not a category!')
            
        # If a name is provided, make sure the name is not already used
        if name:
            if self.has_symbol(name):
                raise Exception('Name \'{}\' is already used!'.format(name))
        
            # Create symbolic representation
            rep = Repr_Symbol(name)
        
        # Construct new object
        X = Object(C)
        if C == Cat:
            X.covariant = True

        # Add object and possible representation to the diagram
        self.add_morphism(X)
        if name:
            rep.assign(X)
            self.add_representation(rep)
        return X
        
    def create_morphism(self, X, Y, name = '', **kwargs):
        # The categories of X and Y must be equal
        if X.category != Y.category:
            raise Exception('Cannot construct a morphism between objects of different categories!')
        
        # If a name is provided, make sure the name is not already used
        if name:
            if self.has_symbol(name):
                raise Exception('Name \'{}\' is already used!'.format(name))
        
            # Create symbolic representation
            rep = Repr_Symbol(name)
        
        # Construct new morphism
        f = Morphism(X.category, X, Y)
        if X.category == Cat:
            f.covariant = kwargs['covariant'] if 'covariant' in kwargs else True
        
        # Add object and possible representation to the diagram
        self.add_morphism(f)
        if name:
            rep.assign(f)
            self.add_representation(rep)
        return f
        
    def create_composition(self, f_list):
        # There must be at least one morphism
        if not f_list:
            raise Exception('Composition requires morphisms!')

        # Obtain domain / codomain
        X, Y = f_list[-1].domain, f_list[0].codomain
        
        # All morphisms must connect
        n = len(f_list)
        for i in range(n - 1):
            if f_list[i].domain != f_list[i + 1].codomain:
                raise Exception('Morphisms do not connect well!')
            
        # Remove all identity morphisms from the list
        f_list = [ f for f in f_list if not f.is_object() ]
                
        # If the list is empty now, then the result would have been id(X) = id(Y)
        n = len(f_list)
        if n == 0:
            return X
        
        # If there is only one morphism to compose, just return that morphism
        if n == 1:
            return f_list[0]
        
        # Create representation
        rep = Repr_Composition(f_list)
        
        # Check if the representation already exists in the diagram, and if so, return the morphism it points to
        g = self.find_representation(rep)
        if g:
            return g

        # Construct new object/morphism
        g = Morphism(X.category, X, Y)
        
        # If we are talking about functors, determine whether the resulting functor is covariant or contravariant
        if X.category == Cat:
            g.covariant = True
            for f in f_list:
                g.covariant = (g.covariant ^ f.covariant)
        
        # Assign, and add morphism and representation to the diagram
        rep.assign(g)
        self.add_morphism(g)
        self.add_representation(rep)
        
        return g
    
    def apply_functor(self, F, x):
        # x must belong to the domain category of the functor
        if(x.category != F.domain):
            raise Exception('Object/morphism does not belong to functor domain!')
        
        # Create representation
        rep = Repr_Functor(F, x)
        
        # Check if the representation already exists in the diagram, and if so, return the morphism it points to
        F_x = self.find_representation(rep)
        if F_x:
            return F_x
        
        # Construct new object/morphism
        if x.is_object():
            F_x = Object(F.codomain)
        else:
            X = x.domain
            Y = x.codomain
            F_X = self.apply_functor(F, X)
            F_Y = self.apply_functor(F, Y)
            if F.covariant:
                F_x = Morphism(F.codomain, F_X, F_Y)
            else:
                F_x = Morphism(F.codomain, F_Y, F_X)
        
        if F.codomain == Cat:
            F_x.covariant = True
        
        # Assign, and add morphism and representation to the diagram
        rep.assign(F_x)
        self.add_morphism(F_x)
        self.add_representation(rep)
        
        return F_x
    
    def apply_property(self, prop, data):
        # TODO: Check if the data satisfies the diagram of the property. If not, return Null / None / False, or something like that

        # Create representation
        rep = Repr_Property(prop, data)

        # Check if this representation already exists somewhere!
        C = self.find_representation(rep)
        if C:
            return C        
        
        # Construct new category
        C = Object(Cat)
    
        # Assign, and add object and representation to the diagram
        rep.assign(C)
        self.add_morphism(C)
        self.add_representation(rep)
    
        return C

    def apply(self, name, data):
        # Try to apply as Property
        prop = self.find_property(name)
        if prop:
            return self.apply_property(prop, data)
        
        # Try to apply as Functor
        F = self.find_morphism(name)
        if F and F.category == Cat:
            if len(data) != 1:
                raise Exception('Can only apply one object/morphism to a functor!')
            return self.apply_functor(F, data[0])
                
        raise Exception('Does not know property or functor {}'.format(name))
    
    def create_equality(self, x, y):
        # x and y must lie in the same category
        if x.category != y.category:
            raise Exception('Objects or morphisms can only be equal if in the same category!')
            
        # Create representation
        rep = Repr_Equality(x, y)
        
        # Check if this representation already exists somewhere!
        C = self.find_representation(rep)
        if C:
            return C
        
        # Construct new category
        C = Object(Cat)
    
        # Assign, and add object and representation to the diagram
        rep.assign(C)
        self.add_morphism(C)
        self.add_representation(rep)
        
        return C
    
    def create_from_placeholders(self, r, M):
        if isinstance(r, Repr_Symbol):
            C = r.ptr.category
            if r.ptr.is_object():
                return self.create_object(M[C] if C in M else C, self.create_name_like(r.name))
            else:
                X, Y = r.ptr.domain, r.ptr.codomain
                covariant = r.ptr.covariant if r.ptr.category == Cat else True
                return self.create_morphism(M[X] if X in M else X, M[Y] if Y in M else Y, self.create_name_like(r.name), covariant = covariant)
        
        if isinstance(r, Repr_Composition):
            return self.create_composition([ (M[f] if f in M else f) for f in r.f_list ])

        if isinstance(r, Repr_Functor):
            return self.apply_functor(M[r.F] if r.F in M else r.F, M[r.x] if r.x in M else r.x)

        if isinstance(r, Repr_Property):
            return self.apply_property(r.prop, [ (M[x] if x in M else x) for x in r.data ])
        
        if isinstance(r, Repr_Equality):
            return self.create_equality(M[r.x] if r.x in M else r.x, M[r.y] if r.y in M else r.y)
        
        print('UNSUPPORTED!!!')
        return None
        
        
#     def create_and(self, C_list):
#         # All C must be categories
#         if any(C.category != Cat for C in C_list):
#             raise Exception('The &-operator only applies to categories!')
            
#         # Create representation
#         rep = Repr_And(P, Q)

#         # Check if this representation already exists somewhere!
#         C = self.diagram.find_representation(rep)
#         if C:
#             return C
        
#         # Construct new category
#         C = Object(Cat)
    
#         # Assign, and add object and representation to the diagram
#         rep.assign(C)
#         self.diagram.add_morphism(C)
#         self.diagram.add_representation(rep)
    
#         return C
    
    
    # --- Non-essential methods ---
    
    def str_x(self, x):
        for r in self.representations:
            if r.ptr == x:
                if isinstance(r, Repr_Symbol):
                    return r.name
                if isinstance(r, Repr_Functor):
                    return '{}({})'.format(self.str_x(r.F), self.str_x(r.x))
                if isinstance(r, Repr_Property):
                    return '{}({})'.format(r.prop.name, ', '.join([ self.str_x(y) for y in r.data ]))
                if isinstance(r, Repr_Composition):
                    return '.'.join([ self.str_x(f) for f in r.f_list ])
                if isinstance(r, Repr_Equality):
                    return '{} = {}'.format(self.str_x(r.x), self.str_x(r.y))

        for ref in self.references:
            s = ref.str_x(x)
            if s != '?':
                return s
        
        return '?'
    
    def __str__(self):
        return '{{ {} }}'.format(', '.join(self.str_x(x) for x in self.morphisms))


# In[9]:


class Context(Diagram):

    def __init__(self):
        super().__init__()
        
        self.data = []
        self.conditions = []
        
    def add_data(self, X):
        self.data.append(X)
    
    def add_condition(self, C):
        # ? TODO ?: check if condition depends only on the data
        self.conditions.append(C)
        
    def find_mapping(self, other_diagram, mapping = {}):
        
        # TODO: OPTIMIZE THIS A LOOOOTTTTT!
        
        for x in self.data:
            if x in mapping:
                continue
            
            x_is_object = x.is_object()
            for y in other_diagram.morphisms: # TODO: should we also look through other_diagram.references ?
                # Categories must match, and objects must be mapped to objects
                if y.category != x.category or (x_is_object and not y.is_object()):
                    continue
                # (Co)domain must match
                if not x_is_object:
                    if x.domain in mapping and mapping[x.domain] != y.domain:
                        continue
                    if x.codomain in mapping and mapping[x.codomain] != y.codomain:
                        continue
                    if not self.owns(x.domain) and x.domain != y.domain:
                        continue
                    if not self.owns(x.codomain) and x.codomain != y.codomain:
                        continue
                
                # Try the mapping
                mapping[x] = y
                if self.find_mapping(other_diagram, mapping):
                    return True
                # If unsuccessful, undo
                del mapping[x]

            return False
        
        # All the data is mapped. Now naturally extend the mapping to all morphisms of the context that depend on the data
        mapping_ext = mapping.copy()
        r_todo = [ r for r in self.representations if r.ptr not in mapping_ext and not isinstance(r, Repr_Symbol) ]        
        updates = True
        while updates:
            updates = False
            r_done = []
            for r in r_todo:
                # Can only extend the mapping to this representation if all its dependencies are already mapped
                if any(x not in mapping_ext and x in self.morphisms for x in r.dependencies()):
                    continue
                                    
                y = other_diagram.create_from_placeholders(r, mapping_ext)
                mapping_ext[r.ptr] = y
                r_done.append(r)
                updates = True
                
            r_todo = [ r for r in r_todo if r not in r_done ]
                        
        # TODO: check if everything is well-mapped!
            
        # Verify conditions
        for C in self.conditions:
            if not other_diagram.has_instance_of(mapping_ext[C]):
                return False
                
        # At this point we know that the mapping works, so update 'mapping'
        mapping.update(mapping_ext)

        return True


# In[10]:


class Property(Context):
    
    def __init__(self, name):
        super().__init__()
        self.name = name


# In[11]:


class Theorem(Context):
    
    def __init__(self):
        super().__init__()
        
        self.conclusion = Diagram()
        self.conclusion.add_reference(self)
        
    def try_application(self, other_diagram, mapping = {}):
        if not self.find_mapping(other_diagram, mapping):
            return False
        
        # Apply conclusion
        r_todo = [ r for r in self.conclusion.representations if r.ptr not in mapping ]
        while r_todo:
            r_done = []
            for r in r_todo:
                if any(x not in mapping and x in self.conclusion.morphisms for x in r.dependencies()):
                    continue
                
                y = other_diagram.create_from_placeholders(r, mapping)
                mapping[r.ptr] = y
                r_done.append(r)
            
            r_todo = [ r for r in r_todo if r not in r_done ]
            
        # Finally, create objects in other_diagram for all objects that do not have a representation in the conclusion (i.e. mostly proofs of statements)
        for x in self.conclusion.morphisms:
            if x not in mapping:
                C = mapping[x.category] if x.category in mapping else x.category
                y = other_diagram.create_object(C)
                mapping[x] = y
        
        return True


# In[12]:


class Example(Diagram):
    
    def __init__(self):
        super().__init__()
        


# In[13]:


class Book(Diagram):
    
    def __init__(self):
        super().__init__()
        
        self.properties = {}
        self.theorems = {}
        self.examples = []
        
    def add_property(self, name, prop):
        self.properties[name] = prop
        
    def has_property(self, name):
        return name in self.properties
    
    def add_theorem(self, name, thm):
        self.theorems[name] = thm
        
    def has_theorem(self, name):
        return name in self.theorems
    
    def find_property(self, name):
        if self.has_property(name):
            return self.properties[name]
        
        for ref in self.references:
            p = ref.find_property(name)
            if p:
                return p
        
        return None


# ### Core objects

# In[14]:


Cat = Object()

_0 = Number(0)
_1 = Number(1)


# In[15]:


def recreate_global_diagram():
    global_diagram = Book()
    global_diagram.add_morphism(Cat)
    global_diagram.add_morphism(_0)
    global_diagram.add_morphism(_1)

    rep = Repr_Symbol('Cat')
    rep.assign(Cat)
    global_diagram.add_representation(rep)

    rep = Repr_Symbol('True')
    rep.assign(_1)
    global_diagram.add_representation(rep)


    rep = Repr_Symbol('False')
    rep.assign(_0)
    global_diagram.add_representation(rep)

    return global_diagram


# In[ ]:




