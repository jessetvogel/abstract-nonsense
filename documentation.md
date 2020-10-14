## Chapter 0 - Notation

- Since objects are considered morphisms (an object $X$ is identified with its identity morphism $\text{id}_X$), we write `X, Y, Z, ...` for objects, `f, g, h, ...` for morphisms that are not objects, and `x, y, z, ...` for morphisms for which itis unknown whether they are objects / identity morphisms.
- Objects which are categories are denoted by `C, D, E, ...`.
- Functors are denoted by `F, G, H, ...`



- Methods starting with `get_` try to find a morphism/theorem/property/whatever by name (string)

  

## Chapter 1 - Core

### 1.1 - Morphisms

- **class** `Morphism`
  - `Morphism(category, domain, codomain)` - creates a morphism in category from domain to codomain
  - `Morphism(category)` - creates an object: its (co)domain is itself
  - `Morphism* category` - the category in which the morphism lives
  - `Morphism* domain, codomain` - the domain and codomain of the morphism
  - `bool is_object()` - indicates whether this is an identity morphism, or equivalently an object (this can simply be checked by checking if the morphism equals its (co)domain)
  - `bool is_functor()` - a morphism is a functor if it lives in the category of categories `Cat`
  - `bool is_category()` - equivalent to `is_object() and is_functor()`
  - `bool covariant` - only used when morphism is a functor
- **class** `Object extends Morphism`
  - *This class is actually not even needed by introduction of `Morphism.is_object()`*
- **class** `Number extends Object`
  - `int n` - the number $n \in \N​$ that it represents
  - *Its category is automatically `Cat`*

### 1.2 - Representations

- **class** `Representation`
  - `Morphism* ptr` - the morphism that it points to
  - `Morphism*[] data` - other morphisms that this representation depends on


Examples of types of representations:

- `RAssumption` - for morphisms that exist by assumption
  - `RDomain` - for objects that exist as the domain of some morphism
  - `RCodomain` - for objects that exist as the codomain of some morphism
  - `RCat` - for objects that exist as the category of some morphism
- `RApplicationFunctor` - for morphisms that arise as the application of a functor
- `RApplicationProperty` - for categories that arise as the application of a property
- `REquality` - for categories that arise as the equality of two morphisms



Question: is every morphism required to have a representation?



### 1.3 - Diagrams

- **class** `Diagram`
  - `Diagram(Diagram*[] references)` - references must be provided upon construction
  - `Morphisms*[] morphisms` - the morphisms that belong to this diagram
  - `Representation*[] representations` - the representations that belong to this diagram
  - `Map<string, Morphism*> symbols` - the symbols or names used to refer to certain morphisms
  - `Diagram*[] references` - diagrams on which this diagram depends
  - `Diagram*[] citations` - diagrams which depend on this diagram
  - `void add_morphism(x)`
  - `void add_representation(rep)`
  - Lots of creation functions `create_[...]`
    - `create_object`
    - `create_morphism`
    - `create_functor_application`
    - `create_property_application`
    - … 
  - `bool has_symbol(name)`
  - `bool add_symbol(name, morphism)`
  - `Morphism* get_morphism(name)`
  - `Property* get_property(name)`
  - `bool knows_instance(C)` - returns true iff it (or any of its parents) has an object in the category `C`

Whenever a morphism or object is created in a `Diagram`, first check if it could have been created in one of its references. If so, let the reference create the morphism instead.

Why do we want *multiple* references? Why is it not sufficient to just have 1 reference? Theorem conclusions have the theorem as their reference, theorems have their book as reference. Same for properties and examples. Do books really need multiple references?



### 1.4 - Contexts, Properties and Theorems

- **class** `Context extends Diagram`
  - `Morphisms*[] data` - these objects are marked as the data of the context. All other morphisms must depend on the data, or on the references
  - `Morphisms*[] conditions` - a list of categories (propositions) that the data is assumed to satisfy
  - `void add_data(x)`
  - `void add_condition(x)`
  - `bool find_mapping(target diagram, mapping = {})` - tries to find a way to map the context to the given diagram given a partial mapping

- **class** `Property extends Context`
  - `string name` - not strictly necessary, 

- **class** `Theorem extends Context`
  - `Diagram conclusion`
  - `bool find_application(target diagram, mapping = {})` - tries to find a way to apply the theorem to the target diagram given a partial mapping

### 1.5 - Books

- **class** `Book extends Diagram`
  - `map<string, Properties> properties`
  - `map<string, Theorem> theorems`
  - `map<string, ...> examples`
  - `void add_property(name, property), add_theorem(name, theorem)`
  - `bool has_property(name), has_theorem(name)`
  - `Property* get_property(name)` - also searches through its references

## Chapter 2 - Grammar and Parsers

- `let X : C` creates an object (by assumption) in the category `C`, and names it `'X'` 

- `property`

- `theorem`

- `example`

- `assume`

- `prove`

  

## Chapter 3 - Mappers



## Chapter 4 - Provers

