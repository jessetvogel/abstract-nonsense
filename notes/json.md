## See `Formatter` for the types of (JSON) messages the program can output



`check theorem affine_then_qcompact`

`check property affine (signature)`

`check example Spec_Z `

### Theorem JSON Format

```json
{
	"type": "theorem",
  "name": "affine_then_qcompact",
  "morphisms": { "X" : { "k": 0, "cat": "Scheme" }, ... },
  "data": [ "X" ],
  "conditions": [ "affine(X)" ],
	"conclusions": [ "qcompact(X)" ]
}
```

### Property JSON Format

```json
{
	"type": "property",
  "name": "affine",
  "signature": [1],
  "morphisms": { "X" : { "k": 0, "cat": "Scheme" }, "Y": { "k": 0, "cat": "Scheme" }, "f": { "k": 1, "cat": "Scheme", "dom": "X", "cod": "Y" }, ... },
  "data": [ "f" ],
	"definition": "X = Spec(Global(X))" // Only if definition is provided
}
```

### Example JSON Format

```json
{
  "type": "example",
  "name": "Spec_Z",
  "morphisms": { "ZZ" : "Ring", ... }
  "assumptions": [ "regular(Spec(ZZ))" ] // ???
}
```



## Messages

- Check morphism `check f`

```json
{
	"type": "morphism",
  "morphism": {
    "f": {
      "cat": "Scheme",
		  "dom": "X",
		  "cod": "Y",
		  "k": 1
    }
  }
}
```



