## Automatic reduction rules

**No actual identifications should be made during the creation of a morphism! This messes up everything!**

Instead, during the creation of a morphism, first search for alternative representations. If there are none, only then create a new morphism. There are (at least) two ways to go about this:

- Have a 'preferred' direction of creating these morphisms (actual **reduction** rules so to say)
- Do some (smart) search for alternative representations: e.g. (`(f.g).h` also checks if `f.g.h` or `f.(g.h)` is already created)





### Immediate rules / cases where not even a Representation is made (These can be checked 'intrinsically': we do not need to look at the representations of the arguments)

- `P = P` gives `True`
- `True = False` gives `False`
- `P & True` gives `P`
- `P & False` gives `False`
- `P | True` gives `True`
- `P | False` gives `P`
- `P -> True` gives `True`
- `False -> P` gives `True`
- `True -> False` gives `False`
- `P & P` gives `P`
- `P | P` gives `P`



- If `F` is an identity functor, then `F(f)` gives `f`



### Indirect rules / cases where identifications should be made (note: but no actual new morphisms are created!)

- Whenever `P -> False` is created, identify `(P -> False) -> False` with `P`
- Whenever `P & Q` is created, identify `(P & Q) -> P` and `(P & Q) -> Q` with `True`
- Whenever `P | Q` is created, identify `P -> (P | Q)` and `Q -> (P | Q)` with `True`



- Whenever a functor application `F(f)` is created, and `f` is of the form `G(g)` for some functor `G`, then identify `F(f)` with `(F.G)(g)`



### Rules for logic inferment

- Whenever `P -> Q` is created [*product/exp adjunction*]
  - If `Q` is of the form `R -> S`, then identify `P -> (R -> S)` with `(P & R) -> S`
  - If `P` is of the form `R & S`, then identify `(R & S) -> Q` with `R -> (S -> Q)` and `S -> (R -> Q)`



