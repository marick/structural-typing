# Change Log
This project adheres to [Semantic Versioning](http://semver.org/).
See [here](http://keepachangelog.com/) for the change log format.

## [in progress]
- BUGFIX: A record on the right-hand side is treated as an exact value, rather than a
  map to traverse to build up paths.
- ADD: Strings can now be used in paths. Like keywords, they denote map keywords to follow.
- ADD: `ONLY` is like `ALL`, but it insists that its collection have exactly one element.
- ADD: By default, a non-function `x` used in predicate position behaves as `(exactly x)`.
       - Regular expressions are treated as in Midje.
       - Comparison of record to map as in Midje.
       - BigDecimal and BigFloat literals will match plain floats, longs, etc.
- CHANGE: Better detection/reporting of structures that cannot match the path.
- CHANGE: Some improvements to error handling
- BREAKING: Removed `required-key` (not bumping major number because this was deprecated
  well before 1.0

## [1.0.1]
- CHANGE: Use newest version of `suchwow` and `specter`.
- BUGFIX: Should now work when in an uberjar.
- Doc tweak via @devn

## [1.0.0]

- ADD: antecedent arguments to `implies` can be condensed type descriptions, not just predicates.
- ADD: `all-built-like` and `<>all-built-like` add indexes to their error messages.
- ADD: Replaced default protocol error messages with clearer ones.
- FIX: `structural-typing` stomped on Specter's handling of keywords.
- FIX: swiss-arrows was only a test dependency

## [0.14.0]

### BREAKING

- You must use `required-path` instead of `required-key`.

### Added
- Considerably better handling of "impossible" path cases. https://github.com/marick/structural-typing/wiki/Error-Cases
- `only-keys` and `exactly-keys` predicates
- `requires-mentioned-paths`

## [0.13.0]

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
