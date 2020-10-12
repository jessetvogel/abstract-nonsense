### Finding mappings

Given diagrams $A$ and $B$ and want to find a (consistent) mapping $\varphi : A \to B$, given an already partial mapping.



- Look at the partial mapping and see if it 'forces' certain other mappings. E.g. if we want `affine(X)` to be mapped to `affine(Y)`, then we probably must map `X` to `Y` (probably requires that some representation (I think `affine(X)` must be unique to what it is pointing to))
- Split $A$ into 'independent components' and find mappings for each one separately
- For each datum $x​$, find a list of possible candidates for $\varphi(x)​$ (if any such list is empty, we directly know there is no mapping!)
  - Filter these list by looking at the conditions
- The first goal is to determine a mapping on all of the data of $A$. Once this is done, the mapping can easily be uniquely extended to $\varphi : A \to B$. Then it must be checked that this mapping is consistent (in some way). Although I do not yet know an example of where this would go wrong..
- 









### Motivating examples

Notation `[X]` indicates that `X` is considered data for the context.

- `{ [X] : Sch, affine(X) : Cat, ? : affine(X) }` with partial mapping `{ affine(X) --> affine(Y) }`
  - Since `affine(X)` is the unique representation for what it points to & it is not data itself, the desired image `affine(Y)` must be of the same form (which it is: if it were not, then we could directly give up). Therefore it is required that `X --> Y`. This determines the map on all data already, so we are done.



- `{ [X], [Y] : Sch, [f] : X -> Y with etale(f) }` with partial mapping `{ etale(f) --> etale(g) }`
  - Same as above to conclude mapping `f --> g`. Then since these are morphisms (they are not their own (co)domain), also map `dom(f) --> dom(g)` and `cod(f) --> cod(g)`



- `{ [R] : Ring with smooth(Spec(R)) }` with partial mapping `{ smooth(Spec(R)) --> smooth(Spec(S)) }`
  - Conclude that `Spec(R) --> Spec(S)` then conclude that `R --> S`



- `{ [R] : Ring, [M] : Mod(R) with flat(R, M) }` with partial mapping `{ M --> N }`
  - Conclude that `Mod(R) --> Mod(S)` (assuming `N : Mod(S)`) and then conclude that `R --> S`

