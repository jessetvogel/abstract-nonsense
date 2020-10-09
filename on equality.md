##### How to set two elements equal in a diagram?

- Whenever you want to set two objects equal, first figure out which diagrams they belong to, and start with the 'youngest' diagram of the two. Let $x$ denote the 'oldest' object, and $y​$ the 'youngest'.

- `Diagram::set_equal(x, y, induced_equalities = [ triples (diagram, x, y) ])` - Now the goal is to replace $y$ with $x$ everywhere. 

  1. If $x$ and $y​$ are already equal, return
  2. **If $y$ in self.conditions, replace by $x$, what about theorems and conclusions???**
  3. For each representation: `for r in ... : ...`
     1. if it points to $y$, let it point to $x$ instead.
     2. if it has dependence on $y$, change the dependence to $x$ (**NO: we require more, the representation has to be recreated!**)
  4. For all morphisms: `for x in ... : ...`
     1. if $y$ is the domain/codomain/category of this morphism, change it to $x$

  3. Run `set_equal(x, y, induced_equalities)` on all children of this diagram
  4. For any duplicate representations `r` and `s`:
     1.  if they point to the same object, just remove one of them.
     2. If they point to different objects, append pointed objects to `induced_equalities` (together with the Diagram which should make this equality). Then remove any one of the two representations.

  5. If $y$ appears anywhere in `induced_equalities`, replace it with $x$ (**is it possible that this happens?**)
  6. If this diagram owns $y$, delete it.
  7. For any triple in `induced_equalities`, if it should be executed by this diagram, then do so. (And of course remove the triple from the list.)



- `Diagram::set_equal(x, y, induced_equalities = [])` - Does the above.

- `Diagram::owner(x)` - Requires that the diagram knows $x$. Returns that the diagram that owns $x$.



##### What is the hierarchy of Diagrams and Objects/Morphisms?

- All diagrams define some kind of tree: each diagram has a parent.