Available via [clojars](https://clojars.org/marick/structural-typing) for Clojure 1.6+  
For lein: [marick/structural-typing "0.4.0"]    
Development version: [marick/structural-typing "0.5.0-SNAPSHOT"]    
License: [Unlicense](http://unlicense.org/) (public domain)        
[API docs](http://marick.github.io/structural-typing/)
 
[![Build Status](https://travis-ci.org/marick/structural-typing.png?branch=master)](https://travis-ci.org/marick/structural-typing)

# structural-typing

As far as looseness and flexibility goes, structural typing lives
between [duck typing](http://en.wikipedia.org/wiki/Duck_typing) and
[nominal typing](http://en.wikipedia.org/wiki/Nominal_type_system).

As implemented here, it's particularly useful for checking data
flowing across module or system boundaries: you check data as it
arrives (possibly coercing or migrating it), then the rest of your
code can assume correctness.

Note that this fits particularly well with a coding style where
programs take in data that is augmented (`assoc`) incrementally by
stages of a pipeline, then transformed into new data that's pumped out
the other side.

Unlike nominal typing, structural types are described by predicates on
keys, rather than by relationships between type names and other type
names. That makes structural typing is especially useful for
loosely-coupled systems.

## Simple examples

This library works with a *type repository* or "type repo". In real
life, I put the repo in one or more namespaces that hold their own
type repositories and define checking functions that work on it. Those
functions are then used like this:

```clojure
(ns myapp.some-namespace
  (:require [myapp.types :as type])
  ...)

... (checked :Point incoming-data) ...

```e

For examples to try in a repl, though, it's convenient to use the
single implicit global repo. Like this:

```clojure
user=> (use 'structural-typing.type)
user=> (use 'structural-typing.global-type)
```

That given, here's how you
define a `:Point` type.

```clojure
user=> (type! :Point {:x integer? :y integer?})
```

Here's an example of a type mismatch:

```clojure
user=> (checked :Point {:x "one" :y "two"})
:y should be `integer?`; it is `"two"`
:x should be `integer?`; it is `"one"`
=> nil
```

By default, type errors are printed as with `println`. That can be
changed; for example, to throw an exception:

```clojure
user=> (on-error! throwing-error-handler)
user=> (checked :Point {:x "one" :y "two"})

Exception :y should be `integer?`; it is `"two"`
:x should be `integer?`; it is `"one"`  structural-typing.api.defaults/throwing-error-handler (defaults.clj:94)

user=> (pst)
Exception :y should be `integer?`; it is `"two"`
:x should be `integer?`; it is `"one"`
	structural-typing.api.defaults/throwing-error-handler (defaults.clj:94)
	structural-typing.api.type-repo.TypeRepo (type_repo.clj:30)
```

Not all that pretty, but I hope you didn't come to Clojure expecting pretty stack traces.

See [TBD: monadic, timbre] to see how to write your own error-reporting functions. And if you're following along at the repl, revert to the original error handler now:

```clojure
user=> (on-error! default-error-handler)
```


The named `checked` is a bit peculiar. That's because it's intended to be used in this sort of pipeline:

```clojure
(defn handler [payload]
  (some-> (type/checked :Point payload)
          frob
          twiddle
          tweak))
```

You place type checks at important boundaries, which pass along
type-checked values to interior functions. When the candidate value
fails the type check, `checked` returns `nil`, so the pipeline is
short-circuited. (Note: if you roll monadically, you can make success
and failure return Either monads. See [TBD].)

In the success case, `checked` returns the unmodified original value:

```clojure
user=> (checked :Point {:x 1 :y 2})
=> {:y 2, :x 1}
```

## Multiple checks for a key


To apply more than one predicate to a key's value, enclose them in a vector:

```clojure
user=> (named! :Point {:x [integer? pos?] :y [integer? pos?]})

user=> (checked :Point {:x -1 :y 2})
:x should be `pos?`; it is `-1`
=> nil

```

It's worth noting that all the predicates are checked even if one of them fails:

```clojure
user=> (checked :Point {:x "string" :y 2})
:x should be `integer?`; it is `"string"`
:x should be `pos?`; it is `"string"`
=> nil
```

If a predicate would throw an error (as `pos?` does on a string), that's considered `false`.


## Optional and required values

In addition to ignoring exceptions, predicates also ignore `nil`
values. The effect is that all keys are optional by default. So all of
these are valid points:

```clojure
user=> (map #(checked :Point %) [{} {:x 1} {:y 1} {:x 1 :y 2}])
=> ({} {:x 1} {:y 1} {:y 2, :x 1})
```

(Note: For this library, a `nil` value is treated the same as a missing
value. There is no difference in the handling of `{:x nil}` and `{}`.)

This optionality seems crazy for points, but it's easy to require a key:

```clojure
user=> (type! :Point {:x [required-key integer?]
                      :y [required-key integer?]})

user=> (checked :Point {:x "1"})
:y must exist and be non-nil
:x should be `integer?`; it is `"1"`
=> nil
```

There is an alternate notation that's shorter and perhaps clearer for
many cases. It lists the the required keys in a vector, separately
from the map of keys to predicates. It looks like this:

```clojure
user=> (type! :Point
              [:x :y]
              {:x integer? :y integer?})
```

Because its use involves some subtlety when it comes to types that include other
types, I won't describe it further here. See TBD.

One final note about optionality. Extra keys are not considered an
error. A value can have as many extra keys as you want and still be
"of" a particular type. That is, all of the following are `:Points`:

```clojure
user=> (map #(checked :Point %) [{:x 1 :y 2}
                                 {:x 1 :y 2 :z 3}
                                 {:x 1 :y 2 :color "red"}])
=> ({:y 2, :x 1} {:y 2, :z 3, :x 1} {:y 2, :color "red", :x 1})
```

### Combining types

Here's one way to create a point with a color:

```clojure
user=> (type! :ColorfulPoint
          {:x [required-key integer?]
           :y [required-key integer?]
           :color [required-key string?]})

user=> (checked :ColorfulPoint {:y 1 :color 1})
:x must exist and be non-nil
:color should be `string?`; it is `1`
=> nil
```

That seems a bit silly, given that we've already defined a `:Point`. Here's how you'd build on that:

```clojure
user=> (type! :ColorfulPoint
              (includes :Point)
              {:color string?})

user=> (checked :ColorfulPoint {:y 1 :color 1})
:x must exist and be non-nil
:color should be `string?`; it is `1`
=> nil

```

If you'll have many colored objects, you can create a `:Colorful` "mixin" and then use it:

```clojure
user=> (type! :Colorful {:color [required-key string?]})
user=> (type! :ColorfulPoint (includes :Point) (includes :Colorful))
```

You might not want to dignify the combination of `:Colorful` and
`:Point` with its own type. To support that, you can check against
more than one type at once:

```clojure
user=> (checked [:Colorful :Point] {:y 1 :color 1})
:x must exist and be non-nil
:color should be `string?`; it is `1`
=> nil
```

It's important to understand that all of the above types are *the
same*. The names are only for human convenience; types are defined by
a structure of keys and predicates, and do not have any intrinsic
relationship to other types.

## Nesting types and key paths

Nested structures are described by nested types. Here's a 
colorful figure composed of a set of points.

```clojure
user=> (def ok-figure {:color "red"
                       :points [{:x 1, :y 1}
                                {:x 2, :y 3}]})
```

We can define its type like this:

```clojure
user=> (type! :Figure (includes :Colorful)
                      {[:points ALL :x] [required-key integer?]
                       [:points ALL :y] [required-key integer?]})
```

You see that you can supply a *path* to a key, not just a
key. Further, that path can include `ALL`, which stands in for all
elements of a collection.

The `ok-figure` matches that type:

```clojure
user=> (checked :Figure ok-figure)
=> {:color "red", :points [{:y 1, :x 1} {:y 3, :x 2}]}
```

Type mismatches are detected as you'd expect:

```clojure
user=> (checked :Figure {:points [{:y 1} {:x 1 :y "2"}]})
[:points ALL :x][0] must exist and be non-nil
:color must exist and be non-nil
[:points ALL :y][1] should be `integer?`; it is `"2"`
=> nil
```

Notice that the paths appear in the output. Notice further that the
paths are suffixed with an index that helps you identify which point
was in error.

Here's an example of the output for a figure that has a point instead of an array of points:

```clojure
user=> (checked :Figure {:points {:x 1 :y 2}})
[:points ALL :x][0] must exist and be non-nil
[:points ALL :y][0] must exist and be non-nil
:color must exist and be non-nil
[:points ALL :x][1] must exist and be non-nil
[:points ALL :y][1] must exist and be non-nil
=> nil
```

It's not as good a description of the real problem as you'd hope for, but it's something.

Other flat-out wrong candidates return better errors:

```clojure
user=> (checked :Figure {:points 3})
[:points ALL :x] is not a path into `{:points 3}`
[:points ALL :y] is not a path into `{:points 3}`
:color must exist and be non-nil
=> nil
```

## For more details

## Todo list

* Coercion and migration
* Friendly printing for Specter operators other than ALL.
* Use for collection checking in Midje 2
* Accurate indexes for multiple uses of ALL (etc.)

-------------------

# Back Matter

## Credits

I was inspired by Elm's typing for its [records](http://elm-lang.org/learn/Records.elm).

I first used type checking of this sort at
[GetSet](http://getset.com), though this version is way
better. (Sorry, GetSet!)

[Specter](https://github.com/nathanmarz/specter) does the work of traversing structures.

## Contributing

Pull requests accepted, provided you have the right to put your contribution into the public domain.
To allow me to be a teensy bit scrupulous, please include the following text in
the comment of your pull request:

    > I dedicate any and all copyright interest in this software to the
    > public domain. I make this dedication for the benefit of the public at
    > large and to the detriment of my heirs and successors. I intend this
    > dedication to be an overt act of relinquishment in perpetuity of all
    > present and future rights to this software under copyright law.

I prefer, but do not require, pull requests with tests. I'm leery of
adding further dependencies to the library, but could be convinced
otherwise.
