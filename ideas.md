- Have *Objects* all of a certain *Type*, each having a set of *Representations / Expressions* (up to isomorphism)
- Such expressions can be built from other objects using *Relations / Functions*



- *Theorems* consist of a context and a conclusion
- *Context* is a list of placeholders to which the theorem applies (this may include proofs/assumptions)

- *Conclusion* is a list of instructions that may be executed to however the context is applied to the workspace



- There is some global *Workspace* containing all Objects, Types, Theorems, etc.

- *Instruction* is a rule to change the workspace

---------------------



### Slogan

*"For proving and disproving abstract nonsense."*



### Category based system

- There are *Objects* and *Morphisms / Arrows*, each living in some *Category*. A *Morphism* also has a *Domain* and *Codomain / Target*, which are objects of the same category.
- Note that we cannot have a fixed number of categories, some categories can depend on objects of another category, e.g. the category of $R$-modules, for some ring $R$.
- Objects and morphisms can have more than one *Representation*. For example, if $f, g, h$ are morphisms, they are represented by the *Symbols* $f, g, h$. It can be that $f \circ g = h$. Every representation will point to a object or morphism, and when equating (i.e. saying two things are isomorphic) to objects or morphisms, we simply merge them so that every representation points to the same thing.
- There are *Functions*, which take a number of objects and/or morphisms, and produce a new object/morphism. [?]
- Note: tensoring $- \otimes_R M$ should be a function that inputs a ring $R$, an $R$-module $M$, and outputs a morphism $R$-Mod $\to $ $R$-Mod. Hence, the type of certain input objects may depend on other input objects.
  - An option is to do this using *Placeholders*? That is, when defining the above function, we could first declare $R$ a placeholder object of $\textbf{Ring}$, and then $- \otimes_R- $ as a map $R$-Mod $\times$ $R$-Mod $\to$ $R$-Mod.
    - But then we need *Products* of categories.
- How will the fiber product $- \times_S -$ be defined? It takes two morphisms $f : X \to S$ and $g : Y \to S$, and produces an object $X \times_S Y$ together with morphisms $X \times_S Y \to X$ and $X \times_S Y \to Y$. This is technically an object in some strange cone category.. Do we really want that?
  - Then we at least need to make $\text{Hom}(-, S)$ a category.
- Note: that not every function will be a functor! E.g. $\text{IsAffine} : \text{Sch} \to \text{Prop}​$ will not be a functor! So we will need 'just functions'.





##### Classes to implement, and what they will be used for:



- Class *Morphism* { Object* domain, Object* codomain }

  - `f : X -> Y` morphisms between objects in a category
  - there are keywords `dom` and `cod` which represent the domain and codomain of the morphism (i.e. use `dom(f)` and `cod(f)`)
  - there is a keyword `id(X)` which yields the identity morphism of an object $X$.

  

- Class *Object* extends *Morphism* { }

  - `Category : Category` the category of categories
  - `Ring, Scheme, Top : Category` other categories
  - `X, Y : Scheme` objects of some category
  - **Note**: an object $X$ is the same as its identity morphism $\text{id}_X$



- ~~Class *Functor* extends *Morphism* { Bool covariant } [Not sure if we actually need this: a Morphism is a Functor precisely if its category is `Category`. Yes, but there will be extra information! ]~~ [But becomes cumbersome to distuingish cases]
  - The $\text{Spec}$ functor `Spec : Ring ~> Scheme  `  (some notation like this to denote covariantness / contravariantness)

  - Forgetful functors, e.g. `Top : Scheme -> Top` yielding the underlying topological space

  - ~~The proofs-functor `Proof / # / @ / $ / [-] : Prop -> Cat ` which sends $P$ to the category of proofs of $P$. Indeed an implication $P \to Q$ yields a functor [Proofs of $P$] $\to$ [Proofs of $Q$]~~ [Propositions are directly considered as categories of their proofs]


- ~~Class *Category* extends *Object*, *Functor* { }~~

  - Enough examples I suppose

    

- Class *Number* extends *Category* { int n }

  - `0, 1, 2, 3, ...`

  - `0` and `1` are also known as `True` and `False`

  - **Note**: a number $n$ is represented by the category consisting of $n$ objects. E.g. `0` is the empty category, and `1` is the category with one identity morphism, etc.

    

- Class *Representation* { Morphism* morphism_that_it_points_to, — data —, bool depends_on(Morphism*) }

  - Symbols `X, R, f, g` 
  - ~~Special keywords `dom(-)`, `cod(-)`, `id(-)`~~ [Those can map directly to the relevant objects/morphisms]
  - Composition of morphisms `f.g.h`
  - Category operators `&` (and/product), `|` (or/coproduct) , `=>` (implies/exponential), `~` (not), `'` (opposite/dual)
  - Functor applications `Spec(R), Top(X), Top(f)`
  - Adjective / Property applications [?] `affine(X), etale(f), cartesian(X, Y, f, g, ...)`
  - **Note**: when creating a representation, the representation may be reduced to a simpler form which is stored instead. Also, when the representation is updated (i.e. when two morphisms are equated), the representation may be reduced even further. However, it should be deterministic in some sense.
    - When `C` and `D` are categories which are natural numbers, the representation of `C => D` will automatically be reduced to `pow(C, D)`. The same holds for `C & D`, `C | D`, `op(C)` etc.
    - Reduce `C => D` to `0` if `D` is `0` and there exists an object in `C`
    - Reduce `C => D` to `1` if `D` is `1` or  `C` is `0`
    - Composition `f . X . g` with (`X` an Object, i.e. identity morphism) reduces to `f # g`

  

- Class *Diagram* { List\<Morphism\*\> morphisms, List\<Representation*\> representations?, List\<Condition\*\> conditions? }

  - Diagrams with a single object
  - Diagrams with a single morphism, and two objects
  - Cartesian squares
  - Compositions of morphisms
  - Used in Theorems, and for Examples
  - Lots of examples here of course
  - **Reminder:** for things like the category of $R$-modules, maybe the diagram could just include $R$. But then there needs to be some function/functor/map `Ring -> Cat`. Well yes! Note that `Mod : Ring* -> Cat` is a contravariant functor! It sends a ring $R$ to the category $R$-Mod. The same holds for `Alg : Ring* -> Cat`.
    - Now of course the problem is how to interpret e.g. tensor products and direct sums.. It seems that we need to associate Diagrams to Diagrams.
      - Possible solution: again it is an adjective! `Tensor(L, M, N)` says whether or not $L$ is the tensor product of $M, N$ (maybe we have to specify additional maps, check the definition..)



- Class *Workspace* { List\<Item\> items, List\<Representation\> reps }
  - Workspace of examples
  - 'Working' workspace



- Class *Adjective / Property* { Diagram* diagram, (String name?) }

  - Adjectives of objects and morphisms `smooth, flat, etale, unramified, ...`
  - Adjectives of squares `pullback, pushout, ...`
  - Adjectives of compositions, maybe, like if we wanted to..
  - **Note**: the 'output' of an adjective is a category! Generally, this is interpreted as the category of proofs of some statement about the category. For example, `affine(X)` is the category of proofs that $X$ is affine.
  - **Note**: the name of a property need not be unique, as long as two properties with the same name have 'non-clashing' diagrams (whatever that means)




- Class *Theorem* { Diagram* setting, List\<Conclusion*\> conclusions }
  - Lots of examples here of course (this is part of what the program is all about!)



- Class *Condition* { — data — }

  - *Equality*: checking if two objects or morphisms are equal

  - *Existence*: checking if a given category has at least one object that we know of (e.g. a proof of a proposition)

    

- Class *Conclusion* { ? } 

  - *Equality*: setting two objects equal (i.e. they are isomorphic)
    - **Note**: really have to be careful with this!
  - *Introduction*: adding an object/morphism to the diagram. E.g. `{ X affine } =(Thm)=> X = Spec(R) for some R : Ring`
  - ~~Adding a proposition to the diagram~~ [This is the same as adding a proof of the statement as an object]



- ~~?Class *Function* { Diagram* input_diagram, Diagram* output_diagram }~~
  - ~~Adjectives of objects and morphisms `smooth, flat, etale, unramified, ...`~~
  - ~~Dimension?~~ If we want to incorporate dimensions, then just let `dim(X)` be the category $\Z$ where $n \in \Z$ denotes a proof that $X$ is of dimension $n$. Although, not sure how to implement it.
  - **NOTE**: to have (counter-)examples, it is important that the properties of an object are really binded to the object!
    - Or more generally, we have a *Diagram* consisting of Objects, Morphisms and conditions on those. Then `cartesian/pullback` could be an Adjective!
    - An *Adjective* could be associated to certain Diagrams in certain categories. E.g. objects can have adjectives (diagram consisting of 1 object), morphisms (diagram consisting of 1 morphism (and 2 objects then as well, right)), and squares (diagram $X, Y, W, S, f : X \to S, g : Y \to S, \pi_1 : W \to X, \pi_2 : W \to Y​$) can be 'cartesian', or a 'pushout'.



- ~~? Class *Category* extends *Object* { Bool has_products, Bool has_coproducts, Bool has_exponents, Bool has_initial, Bool has_final, etc. }~~
  - ~~Then `Prop` can be considered a category, which may be nice?~~
- ~~? Class *HeytingAlgebra* extends *Category* {  }~~



**Default / Core objects**

- `Cat / Category` the category of categories, its category is itself.

- `0, 1` the empty category and the category of one element. They are also denoted `True` and `False`. Any natural number $n \in \N$ should automatically be associated a category, but of course they cannot exist all by default due to not having infinite memory.

  

##### How to deal with Propositions?

1. Treat propositions completely separately from categories, i.e. propositions are not objects in a category, but just objects of class Prop.

2. ~~Treat Prop also as a category, and therefore something like `affine(X)` is actually an object in a *Diagram*.~~
3. Treat the output of an Adjective as a Category! In particular, an object of the category `affine(X)` represents a proof that $X​$ is affine. Then we can actually have the proofs be part of the diagram.

   - Then: how to deal with connectors like `&`, `|`, `=>`, `~` ?

     - They can be implemented as operators on Categories, creating new categories. They follow certain rules.

   - Might actually let `Prop` be equal to `Cat`, just a different representation? Don't see the use of having propositions outside of them being proofs.

     



##### How to deal with dual/opposite categories?

1. Treat them as separate categories, have a relation `op(-)` both for categories and `op(-)` for morphisms. **Note:** the dual category is not the same as the dual object in $\text{Cat}^\text{op}$.

2. A morphism in a category is stored as the same python-object as the dual morphism in the opposite category.

   **Problem**: if we e.g. let $R, S$ be objects in $\text{Ring}^\text{op}$, and take a morphism $R \to S$, what is its direction?

3. Actually have two different categories, and two different types of morphism, but use coercion when asked for (e.g. we can write `Spec(R)` instead of `Spec(R')`)

   1. Whenever a dual category is introduced, automatically indicate that the dual of the dual is itself.

4. Somewhere store a boolean which indicates whether or not an object/morphism is dual or not

   1. Does this cause problems for self-dual categories? It would be very weird to say that `C = C'` and then ask what identifications to make on morphisms?

5. Do not implement such a thing as dual categories in the core of the program. Since we mostly will use it just for expressing covariance/contravariance of functors! Just have a notation for contravariant functors, and a notation for an anti-equivalence of categories. (This does require an extra class `Functor extends Morphism`. We might as well complete the diamond by saying that `Category` is a `Morphism` that is both a `Functor` and an `Object`). But then a `Category` can suddenly be contravariant? ~~What does this mean? Well, such a $\mathcal{C}$ represents a contravariant functor $\mathcal{C} \to \mathcal{C}$. So $\mathcal{C}(f) : \mathcal{C}(y) \to \mathcal{C}(x)$ for all $f : x \to y$.~~  [No, we just do not allow this to happen. Categories are as functors always covariant.]
   ~~What if we say that a functor reverses morphisms if its 'covariantness' disagrees with the covariantness of the input and output?~~ [Cumbersome]
   ~~This seems to cause more problems than it solves actually. Or does it? What if we just say that categories are never contravariant? It only seems not elegant.~~ [But probably the best solution]

- **Note**: the 'not' functor `Cat -> Cat` is contravariant: if $P \to Q$, then $\sim{Q} \to \sim{P}$. Actually, one defines `~` to be `(-) => 0`. But I don't think that we want $\sim{P}​$ to be considered inside `Cat^op` that would be weird.
- **Note:** we also have the functor `op : Cat -> Cat` which is covariant ($\mathcal{C} \to \mathcal{D}$ yields $\mathcal{C}^{\text{op}} \to \mathcal{D}^\text{op}$) and squares to the identity!
- The problem is, we add so much information, while there really is no extra information. We just invert some arrows sometimes.
- **Note**: an equivalence of categories is not the same as an isomorphism, right? Say we ever want to express an equivalence of categories, how would we do so? It is an option still to express it as an equality though.



##### What functionality we want to be able to implement:

- `R : Ring`, `X, Y : Scheme` to define objects of a given category
- `Ring, Scheme, Top, (Set?): Category` including categories themselves
- `f : X -> Y` to define morphisms between objects (must be of the same category)
- `Spec : Ring ~> Scheme` to define functors between categories (think about how to encode covariantness/contravariantness). They can be stored as a (*Functor* extends *Morphism*)?
- `X = Spec(R)` will be of type `Scheme` (same for morphisms).



- ~~`P, Q : Prop`~~ do propositional logic as well `P, Q : Category`
- `R = P & Q;   S = P | Q;   T = P => Q;   U = ~P`
- Do numbers! `n = 3; m = 4; n + m / n | m; n * m / n & m; n ^ m / m => n (note the order)`



- `M, N : Mod(R)`
- ~~`Tensor(M, N) `~~
- ~~`F = Tensor(-, M)` which will be of type `Mod(R) -> Mod(R)`~~



- ~~(Co)limits~~ (replaced by saying that a diagram is a limit or not)

  - Limits: Pullbacks (products), equalizers

  - Colimits: Pushouts (coproducts), coequalizers

    

**Instructions:**

- `assume Q` (this amounts to creating a (representation-less) object (i.e. proof) of the given category)
- `prove P => Q` (this amounts to finding an object (i.e. proof) of the given category)

**Not sure about:**

- ~~How to implement adjectives of objects / morphisms?~~

  - They can either be expressed as *Functions* with data about input and output categories, and whether they should be morphisms 

  - Or they are expressed as functors `Ob(Scheme) -> Prop` or `Hom(Scheme) -> Prop`

    - ~~`affine, separated : Obj(Scheme) -> Prop ` will be stored as a *Function* from `Scheme` to `Prop`~~
    - ~~`etale, flat : Mor(Scheme) -> Prop`~~
    - But then, what is `Ob` and what is `Hom` ?

  - Or they are special classes itself *Adjective* { Object* category, Bool object_or_morphism }

    

- ~~`f : X -> S; g : Y -> S; Z = FiberProduct(f, g) ` ??? **Very problematic!** What even is its type? Because it comes with projection maps etc.~~











