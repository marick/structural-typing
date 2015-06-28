(ns structural-typing.oopsie
  "The declaration of the core data structure: the oopsie. It is produced when a
   predicate is applied to a value and fails.
   
   * **whole-value**: the original value passed to [[checked]].
   * **leaf-value**: the value passed to the predicate.
   * **path**: A Specter-style path.
   * **leaf-index**: When the path matched multiple elements, this is the
     index of the leaf-value in the resulting collection. Note that a 
     path like `[:x ALL ALL ALL :y]` produces a flat collection, so the
     index is not so useful in that case. 
   * **leaf-count**: The number of leafs produced by the path. (Strictly,
     even a path like `[:x :y]` produces a singleton collection.)
   * **predicate**: the original predicate (any callable)
   * **predicate-string**: a friendly string, such as `even?` instead
     of `#<core$even_QMARK_ clojure.core$even_QMARK_@47a01b6e>`
   * **predicate-explainer**: The explainer function associated with the
     predicate. It is applied to the oopsie to produce a string.
")


;;; TODO: unravel dependencies enough to use `checked` this project's code.
