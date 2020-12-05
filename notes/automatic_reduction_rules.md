## Automatic reduction rules



### Immediate rules / cases where not even a Representation is made

- `P = P` gives `True`
- `True = False` gives `False`
- `P & True` gives `P`
- `P & False` gives `False`
- `P | True` gives `True`
- `P | False` gives `P`
- `P -> True` gives `True`
- `False -> P` gives `True`
- `True -> False` gives `False`



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



