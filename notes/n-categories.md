- There is only one $(-2)$-category
- A $(-1)$-category is a truth value (i.e. true or false)
- A $0$-category is a set
- A $1$-category is an ordinary category: contains objects and morphisms (e.g. Ring, Scheme, Top, ...)
- A $2$-category contains: objects, 1-morphisms and 2-morphisms (e.g. Cat)



- An $n$-category contains $k$-morphisms for $0 \le k \le n$



- `Prop` is a $0$-category (i.e. a set)
- `P : Prop` | `P` is a $(-1)$-category of the $0$-category `Prop`



- `P -> Q` | this is $\text{Hom}_{\texttt{Prop}}(P, Q)$ is also a $(-1)$-morphism in `Prop`.



- `Cat` is a $2$-category: the category of $1$-categories
- A $1$-category $\mathcal{C}$ is a $0$-morphism in `Cat` ($1$-dom = $1$-cod = $2$-dom = $2$-cod = $\mathcal{C}$)
- A functor $F : \mathcal{C} \to \mathcal{D}$ is a $1$-morphism in `Cat` ($1$-dom = $\mathcal{C}$, $1$-cod = $\mathcal{D}$, $2$-dom = $2$-cod = $F$ itself)
- A natural transformation $\mu : F \Rightarrow G$ is a $2$-morphism in `Cat` ($2$-dom = $F$, $2$-cod = $G$)



- `Ring` is a $1$-category
- A ring $R$ is a $0$-morphism in `Ring` ($1$-dom = $1$-cod = $R$)
- A homomorphism $f : R \to S$ is a $1$-morphism in `Ring` ($1$-dom = $R$, $1$-cod = $S$)



- `Set` is a $1$-category: the category of $0$-categories
- A set $S$ is a $0$-morphism in `Set` ($1$-dom = $1$-cod = $S$)
- A map $f : S \to T$ is a 1-morphism in `Set` ($1$-dom = $S$, $1$-cod = T)



- `Prop` is a $0$-category: the category of $(-1)$-categories
- A proposition $P$ is a $0$-morphism in `Prop`



- `True` is a $(-1)$-category: the category of $(-2)$-categories
- `False` is a $(-1)$-category



- $\text{Hom}_{\texttt{Cat}}(\mathcal{C}, \mathcal{D})$ is a $1$-category
- $\text{Hom}_{\texttt{Cat}}(F, G)$ is a $0$-category
  - its $0$-morphisms are natural transformations $\mu : F \Rightarrow G$, which are also $1$-morphisms of $\text{Hom}_{\texttt{Cat}}(\mathcal{C}, \mathcal{D})$, which are also $2$-morphisms of `Cat` = $\text{Hom}_{\texttt{Cat}}(\texttt{Cat}, \texttt{Cat})$ ?


- $\text{Hom}_{\texttt{Ring}}(R, S)$ is a $0$-category


- $\text{Hom}_{\texttt{Prop}}(P, Q)$ is a $(-1)$-category



- $\text{Hom}_{n\text{-cat}}(k\text{-mor}, k\text{-mor})$ is a $(n - k - 1)$-category





- $k = -1$ morphisms are the category themself?
- $\text{Hom}_{\texttt{Cat}}(?, ?) = \texttt{Cat}$





- `Morphism { Category cat?; int n; int k; Morphism dom, cod; }`
- `Category extends Morphism { int n; }`
- `Hom extends Category { int p; Morphism domain, codomain; }`



- `Map<Morphism, Morphism> cat`



- $\mathcal{C}$ has n = 2, k = 0
- $F$ has n = 2, k = 1
- $\mu$ has n = 2, k = 2 
- Hence, `Cat` has n = 2, k = -1???



- $R$ has n = 1, k = 0
- $f$ has n = 1, k = 1
- Hence `Ring` has n = 1, k = -1???



Cat = Hom(p = 3, n = 2, domain = codomain = self)

Ring = Hom(p = 2, n = 2, domain = codomain = self)

Set = Hom(p = 2, n = 1, domain = codomain = self)

Prop = Hom(p = 1, n = 0, domain = codomain = self)

True = Hom(p = 0, n = -1, domain = codomain = self)







- `Cat` = $1$-Cat
- `Set` = $0$-Cat
- `Prop` = $(-1)$-Cat
- `True` = $(-2)$-Cat



