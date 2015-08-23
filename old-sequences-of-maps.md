## Sequences of maps

Suppose your input were a sequence of points. You could define a `:Points` type like this:

```clojure
user=> (type! :Points {[ALL :x] integer?
                       [ALL :y] integer?})
```

This shows that the map format for defining a type doesn't necessarily
mirror the "shape" of the type. Instead, the map links a *key path* to
a (partial) description of what should be found when you choose that
path. In the above, the `ALL` means that what follows should apply to
all of the elements of what must be a collection. What if it's not a collection?

```clojure
user=> (checked :Points 3)
[ALL :x] is not a path into `3`
[ALL :y] is not a path into `3`
=> nil
```

If the argument is a collection, each element is "descended" using the
element (in this case, a key). And if it's not an associative structure...?

```clojure
user=> (checked :Points [1 2 3])
=> nil
```

Darn.
