# Change Log
This project adheres to [Semantic Versioning](http://semver.org/).
See [here](http://keepachangelog.com/) for the change log format. 

## [unreleased]

### BREAKING

- You must now use `requires` instead of the vector notation for required paths.
- "Forking" paths must be indicated by `through-each` or `each-of`, not vectors.

### Deprecated

- Use `built-like` instead of `checked`.
- Use `build-like?` instead of `described-by?`

### Added

- When checking a type, you can give "on the fly" condensed type descriptions in addition
  to type signifiers.
- A keyword `:a` in a condensed type description is equivalent to `[:a]`.
- `implies` to `structural-typing.preds`.
- `doc` namespace.
- Predicates can be applied to the whole structure.
- `origin` and `description` let you see the definition of a type.
- Predicate sequences can include types (convenience)
- You can refer to the paths within a type or map with `paths-of`.
- You can use integers in paths `{[:a 1 :b] even?}`

### Changed

- Namespaces reshuffled so all the pieces needed for writing custom predicates
  are in one subdirectory, `assist`.
