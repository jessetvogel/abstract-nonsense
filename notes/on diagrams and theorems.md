### Where do diagrams appear?

- ~~**Book**~~ **Session**: objects, representations, properties, theorems, examples
- **Theorem**: objects, representations, data, conditions, conclusions
  - Think of '*conclusions*' as a diagram on top of the context of the theorem. A theorem then means that any diagram can be completed to a larger diagram.
- **Property**: objects, representations, data, conditions
- **Example**: objects, representations



- class *Diagram* { objects, representations, — methods for constructing new objects / representations (what is now Factory) —, parent(s?) / 'references?', children[]/'citations' }
- class *Context* extends *Diagram* { data, conditions }
  - Can be 'cleaned up': removing all objects/morphisms/representations that do not depend on the data
- class *Property* extends *Context* { }
- class *Theorem* extends *Context* { Diagram conclusions }
- class *Example* extends *Diagram* {}
- class ~~Book~~ *Session* extends *Diagram* { Map\<String, Property\> properties, Map\<String, Theorem\> theorems, List\<Example\> examples }









- ~~(Allowing a diagram to have multiple parents can make it easier to verify conditions without polluting diagrams to which you want to apply a theorem (when it is not applicable).)~~ No, I think you can think of a workaroud if you really want to, but also, I don't think it matters



