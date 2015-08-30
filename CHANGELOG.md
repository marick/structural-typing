# Change Log
This project adheres to [Semantic Versioning](http://semver.org/).
See [here](http://keepachangelog.com/) for the change log format. 

## [unreleased]

### Added

- a keyword `:a` in a condensed type description is equivalent to `[:a]`.
- `implies` to `structural-typing.preds`.
- `doc` namespace.
- Predicates can be applied to the whole structure.
- `origin` and `description` let you see the definition of a type.

### Changed

- Namespaces reshuffled so all the pieces needed for writing custom predicates
  are in one subdirectory, `pred-writing`.
