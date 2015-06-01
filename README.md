Available via [clojars](https://clojars.org/marick/structural-typing) for Clojure 1.4+  
For lein: [marick/structural-typing "0.1.0"]    
Development version: [marick/structural-typing "0.2.0-SNAPSHOT"]    
License: [Unlicense](http://unlicense.org/) (public domain)        
[API docs](http://marick.github.io/structural-typing/)
 
[![Build Status](https://travis-ci.org/marick/structural-typing.png?branch=master)](https://travis-ci.org/marick/structural-typing)


# structural-typing

This library provides an easy way to say "a given map must contain at
least these specific keys". For example, here's a global definition of the structural type `:point`:

```clojure
(require '[structural-typing.type :as type])

(type/named! :point [:x :y])
```

(There are also variants of these functions that don't mutate a global "type repo". Instead, you pass a type repo to various functions.
Or, since the type repo is always the first argument, you use `partial` to handle the bookkeeping.
See the [API docs](http://marick.github.io/structural-typing/).)


We can now ask if a given map or record is of that type:

```clojure
(type/instance? :point {:x 1, :y 1}) => true
(type/instance? :point {:x 1, :y 1, :color :green}) => true ; green points are points 
(type/instance? :point {:x 1, :y 1, :z 1}) => true ; extra dimensions don't destroy "pointhood"
(type/instance? :point {:x 1} => false
```

More common than `instance?` is `checked`, which normally just returns what it was given:

```clojure
(type/checked :point {:x 1, :y 1}) => {:x 1, :y 1}
```

In the case of error, it returns `nil`, which is useful in constructs like this:

```clojure
(some-> (type/checked :frobnoz x)
        (assoc :goodness true)
        ...)
```

It also prints an error, via `println`. Most likely, that's not the error notification you want.
For example, you might want any type failure to throw an exception. That's done like this:

```clojure 
(type/set-failure-handler! type/throwing-failure-handler)
```

More generally, `set-failure-handler!` should be set to any function that
takes a list of error messages. For example, I use
[Timbre](https://github.com/ptaoussanis/timbre) to handle logging, so my failure handler
maps `taoensso.timbre/error` over the failure handler's argument.

Another alternative, if you swing categorically, is to use something
like the Either monad. Here's an example that uses 
Armando Blancas's [Morph](https://github.com/blancas/morph) library.
As is common, we'll interpret the result of a computation to have
either a "left" value (with data about an error) or a "right" value
(with the computation's result). Those are plugged into our type repo as follows:

```clojure
(require '[structural-typing.type :as type])
(type/set-failure-handler! m/left)
(type/set-success-handler! m/right)
```

Let's register a type checker for maps that must contain `:a` and `:b`:

```clojure
(type/named! :ab [:a :b])
```

Here are some results:
```clojure
user=> (str (type/checked :ab {:a 1}))
"Left (b must be present)"
user=> (str (type/checked :ab {:a 1, :b 1}))
"Right {:b 1, :a 1}"
```

One purpose of an error-handling monad is to easily filter out error values as they pass through a computation. Here's a simple example:

```clojure
;; Get both types of results
(def result (map #(type/checked :ab %) [{:a 1} {:b 2} {:a 1 :b 2} {:a 1 :b 2 :c 3}]))

;; Extract the successful values
user=> (m/rights result)
({:b 2, :a 1} {:c 3, :b 2, :a 1})

;; Extract the failures, which note are in a list of lists
user=> (map println (m/lefts result))
(b must be present)
(a must be present)
(nil nil)
```

That output is not so satisfying because the error messages don't
identify which map they apply to. That can be fixed by setting the
type repo's *formatter*. It takes two arguments. The first is a map
from key to error messages:

```clojure
{:a ["b must be present"]}
```

The second is the map being checked. We can use that to produce, for
each map, a list containing all the error messages with the original at its head:

```clojure
(type/set-formatter! (fn [errors-by-key original]
                       (cons original (flatten (vals errors-by-key)))))
```

The result looks like this:

```clojure
;; need to get a new set of lefts.
user=> (def result (map #(type/checked :ab %) [{:a 1} {:b 2} {:a 1 :b 2} {:a 1 :b 2 :c 3}]))
user=> (m/lefts result)
(({:a 1} "b must be present") ({:b 2} "a must be present"))
```

## Coercions

I use structural type checking on the boundaries of [bounded contexts](http://martinfowler.com/bliki/BoundedContext.html).
However,
pure type checking is often not enough. We'd like different bounded
contexts to have different representations 
that evolve at different rates. For example, the "bookkeeping" bounded context might have a different structure for "user" than the "warehouse" context does.

That means that type
conversion may need to take place on values flowing into a
context. That's done with the `coerce` function.

Here's an example of renaming a key. It uses the `rename-keys` function which, for some unknown reason, is in `clojure.set`.

```clojure
(type/start-over!) ; this empties the type repo and resets the success, failure, and formatter functions.
(type/named! :ab [:a :b])
(type/coercion! :ab (fn [from] (clojure.set/rename-keys from {:aa :a})))
```

We can now handle either old-format or new-format maps:

```clojure
user=> (type/coerce :ab {:aa 1, :b 2, :c 3}) ; old format
{:a 1, :c 3, :b 2}

user=> (type/coerce :ab {:a 1, :b 2, :c 3}) ; new format
{:c 3, :b 2, :a 1}
```

Note that the result of the `coercion` function is also checked:

```clojure
user=> (type/coerce :ab {:aa 1})
b must be present
nil
```

So the ordering of events is:

1. Possibly malformatted map arrives.
2. It is corrected.
3. It is checked.

Note: if the coercion function has to handle more than one incoming
type, all the logic to choose between them has to be within it. I
can imagine a version of `coercion` that takes a map of type names
to conversion functions, but that hasn't been written.

## Using more than key names for types

This library uses [Bouncer](https://github.com/leonardoborges/bouncer)
under the hood. At some point, I'll expose more of that so that you
can, for example, declare that `:x` and `:y` must both be floats.

## Credits

I was inspired by Elm's typing for its [records](http://elm-lang.org/learn/Records.elm).

[Structural typing](http://en.wikipedia.org/wiki/Structural_type_system) is a respectable variant of typing, standing between name-based typing (which only is only indirectly about structure) and duck typing (which only cares about a particular field at a particular time). 

I first used type checking of this sort at
[GetSet](http://getset.com), though this API is better. (Sorry,
GetSet!)

[Bouncer](https://github.com/leonardoborges/bouncer) does the actual checking.

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

