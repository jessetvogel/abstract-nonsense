### Priority

- ~~Move `Diagram::str` to `Session:str` (which looks at the owner)~~

- ~~Make `write` a statement~~

- Implement some command that lists theorems, examples, and properties

- ~~A way to reference morphisms in an example:~~
  
  - ~~`example::f`~~ This will mess up morphism ownership!
  
  - ```
    example ZZ {
    	// Commands executed from within ZZ
    }
    ```
  
    which can be opened multiple times!
  
    - Can we `import` from within an example? Yes, and all is executed in the example!
    - Can we define properties/theorems/examples or search from within an example? Let's not do that!



- How to deal with modules? Cannot do `Mod : Ring -> Cat` anymore since `Ring` is a 1-category and `Cat` a 2-category.
  - Maybe generalize `Property` to yield n-Categories instead of only (-1)-Categories (`Prop`'s)
    - `class Property { String name; Diagram diagram; int n; }` -- application creates an $n$-category
    - But why stop there? We can replace `int n` by `Morphism cat;` which creates an object in the (mapped) `cat`.
      - But this is just type theory, complicated very much..



### To Implement

- ~~A way to load files? `load schemes` or so.. / `import "filename"`~~
- ~~A efficient way to find mappings~~
- **How to deal nicely with composition of morphisms?**
- ~~A way to create examples, and to search for examples~~
- ~~Take a look at the creation of new objects/morphisms. Apply some reduction rules, and try to descent (if a reference knows all the data as well, create the instance over there instead)~~

- ~~`Number +/&/=> Number` should just give another `Number`~~

- ~~Applying equalities.~~

- ~~A rule to shorten expressions like `etale(X, Y, f)` to just `etale(f)` ?~~
  - ~~**Note:** it must be consistent then: either use `etale(X, Y, f)` everywhere, or use `etale(f)` everywhere (preferably the latter of course). Because otherwise it would be very hard to match two two if ever needed.~~
  - ~~A possibility is to use/allow a dash `_` in defining morphisms, e.g. `f : _ -> _​`, and also REQUIRE that every object has at least one representation: then 'being the (co)domain' of `f` should also be a representation type, as well as 'is a proof'.~~
    - ~~But then how to specify the category of a morphism `_ -> _`~~
  - ~~Solved by `use`, which creates implicit data~~



- How to prove `P -> Q` statements, and how to prove `P | Q` statements?
  - Option 1: Make `Prover` a `Diagram`, where additional hypotheses can be made. Then `P => Q` can be proven by assuming `P` (i.e. creating another Prover on top of this, and try to prove P) over there.
  - Option 2: Heyting algebra approach
  - Option 3: strategies for particular cases: looks like option 2, but a bit different



- ~~Have `Definitions` ? E.g. `etale(f)` is defined as `formally_etale(f) & locally_finite_presentation(f)`~~

- ~~`Definition` / aliasing | for `affine(X) <=> X = Spec(Ring(X))`~~
- ~~Property signatures, so that affine can hold for schemes and scheme morphisms~~

