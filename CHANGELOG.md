# Change Log
This project adheres to [Semantic Versioning](http://semver.org/).
See [here](http://keepachangelog.com/) for the change log format.

## [2.0 - in progress]
- CHANGE: Way better detection/reporting of structures that cannot match the path
  (truncated and misshapen structures).
- CHANGE: `reject-nil` and `reject-missing` give better control than `required-path`
  (which still exists)
- ADD: Strings can now be used in paths. Like keywords, they denote map keywords to follow.
- ADD: `ensure-standard-functions` makes it easier to write namespaces containing type repos.
- BREAKING: Removed deprecated functions `required-key` and `checked`
- BREAKING: Will no longer reject a top-level nil unless you tell it to.
- BREAKING: Is much better at discovering and describing truncated structures that
  don't match the type declaration. This could lead to new or different error messages.
- BREAKING: helper function `compose-predicate` is now `compose-checker`
- NOTE: an earlier version of 2.0 allowed literals. That was a bad idea.

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
