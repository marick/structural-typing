Available via [clojars](https://clojars.org/marick/structural-typing) for Clojure 1.4+  
For lein: [marick/structural-typing "0.2.0"]    
Development version: [marick/structural-typing "0.3.0-SNAPSHOT"]    
License: [Unlicense](http://unlicense.org/) (public domain)        
[API docs](http://marick.github.io/structural-typing/)
 
[![Build Status](https://travis-ci.org/marick/structural-typing.png?branch=master)](https://travis-ci.org/marick/structural-typing)


## Typing driven by the existence of keys

This library provides an easy way to say "a given map must contain at
least these specific keys". It's intended mainly for use at the
boundaries of libraries or services, where errors are logged to be
viewed by programmers. It can also be used to generate validation
errors to be shown to the user, though libraries like
[Bouncer](https://github.com/leonardoborges/bouncer) or
[Validateur](http://clojurevalidations.info/) may fit better.

### Case 1: a service

Suppose you have a service of some sort that runs as an independent
Clojure process. You'll most likely have a namespace that describes
the types of structures that flow into and out of the service. Let's
suppose that there's only one such type, a `:point`:

```clojure
(ns munger.types 
  (:require [structural-typing.global-type :as global-type]))

(defn setup! []
  (global-type/named! :point [:x :y]))

(defn teardown! []
  (global-type/start-over!))
```

Suppose that some handler function is supposed to consume a point that comes across AMQP. The handler can be written like this:

```clojure
(ns munger.point-service
  (:require [structural-typing.type :as type]))
  
(defn handler [payload]
  (some-> (type/checked :point payload)
          frob
          twiddle
          tweak))
```

If the `payload` contains `:x` and `:y`, it will be
frobbed, twiddled, and tweaked, and the result will be returned. 

If, however, either `:x` or `:y` are missing, this error message will be printed to standard output:

```
:y must be present and non-nil
```

Also, the rest of the pipeline will be skipped and `nil` will be the final result (because of `some->`).

It's quite likely a simple `println` of output won't suffice. Suppose
you (like me) use Peter Taoussanis's
[Timbre](https://github.com/ptaoussanis/timbre) logging library. In that case, you can do the following:

```clojure
(ns munger.types 
  (:require [structural-typing.global-type :as global-type]
            [taoensso.timbre :refer [error]]))

(defn setup! []
  (global-type/named! :point [:x :y])
  (global-type/set-failure-handler! #(doseq [msg %] (error msg))))
    
```

```clojure
user=> (type/checked :point {:x nil :y 1})
2015-Jun-08 15:28:11 -0500 busted ERROR [user] - :x must be present and non-nil
nil
```

### Important notes

1. The type checker allows extra keys. That is, both `{:x 1, :y 1}` and `{:x 1, :y 1, and :z 3}` are considered
   points.

2. `nil` values are always an error. It doesn't matter if the `nil` was an explicit value of a key or the default default value.
   That is, the following map is also an incorrect point: `{:x nil, :y 1}`.


### Case 2: a namespace

Suppose the `:point` type is only relevant to a particular namespace. You can scope point-checking to that namespace
like this:

```clojure
(ns munger.lib
  (:require [structural-typing.type :as type]))

(def type-repo (-> type/empty-type-repo
                   (type/named :point [:x :y])
                   (assoc :failure-handler #(doseq [msg %] (error msg)))))

(def type-checked (partial type/checked type-repo))

(defn handler [payload]
  (some-> (type-checked :point payload)
          frob
          twiddle
          tweak))
```


### Case 3: For the monadically inclined

Another alternative, if you swing categorically, is to use something
like the Either monad. Here's an example that uses 
Armando Blancas's [Morph](https://github.com/blancas/morph) library.
As is common, we'll interpret the result of a computation to have
either a "left" value (with data about an error) or a "right" value
(with the computation's result). Those are plugged into our type repo as follows:

```clojure
(require '[blancas.morph.monads :as m])
(global-type/start-over!)
(global-type/set-failure-handler! m/left)
(global-type/set-success-handler! m/right)
```

Let's register a type checker for maps that must contain `:a` and `:b`:

```clojure
(global-type/named! :ab [:a :b])
```

Here are some results:
```clojure
user=> (type/checked :ab {:a 1})
Left (:b must be present and non-nil)
user=> (type/checked :ab {:a 1, :b 1})
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
(:b must be present and non-nil)
(:a must be present and non-nil)
(nil nil)
```

That output is not so satisfying because the error messages don't
identify which of the four input maps they apply to. That can be fixed by setting the
type repo's *map-adapter*. It takes two arguments. The first is a map
from key to a sequence of error messages:

```clojure
{:b [":b must be present and non-nil"]}
```

The second is the map being checked. We can use that to produce, for
each map, a list containing all the error messages with the original at its head:

```clojure
(global-type/set-map-adapter! (fn [errors-by-key original]
                                (cons original (flatten (vals errors-by-key)))))
```

The result looks like this:

```clojure
;; need to get a new set of lefts.
user=> (def result (map #(type/checked :ab %) [{:a 1} {:b 2} {:a 1 :b 2} {:a 1 :b 2 :c 3}]))
user=> (m/lefts result)
(({:a 1} ":b must be present and non-nil") ({:b 2} ":a must be present and non-nil"))
```

### Instance checks

If you want boolean results rather than out-of-band error messages, use `instance?`:

```clojure
(type/instance? :point {:x 1, :y 1}) => true
(type/instance? :point {:x 1, :y 1, :color :green}) => true ; green points are points 
(type/instance? :point {:x 1, :y 1, :z 1}) => true ; extra dimensions don't destroy "pointhood"
(type/instance? :point {:x 1} => false
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
(global-type/named! :ab [:a :b])
(global-type/coercion! :ab (fn [from] (clojure.set/rename-keys from {:aa :a})))
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
:b must be present and non-nil
nil
```

Because of this, you can use `coerce` in the sort of pipelines that `checked` was used for above:

```clojure
(some-> (type/coerced :point payload)
          frob
          twiddle
          tweak))
```

Note: if the coercion function has to handle more than one incoming
type, all the logic to choose between them has to be within it. I
can imagine a version of `coercion` that takes a map of type names
to conversion functions, but that hasn't been written.

## Checking the types of values

Although checking the existence of keys is sufficient for many cases,
it's sometimes useful to require that certain predicates be true of
values. For example, here's how you declare that only even numbers are accepted:

```clojure
(global-type/named! :even-point [:x :y] {:x even? :y even?})
```

The vector argument denotes the required keys. The following map argument, if any, gives predicates to be run over those keys.
Here's an example of a failure:

```clojure
user=> (type/checked :even-point {:y 1})
:x must be present and non-nil
Custom validation failed for :y
nil
```

The second message is not exactly ideal. In such cases, use vars instead of functions. If the map were `{:x #'even? :y #'even?}`, the
output would look like this:

```clojure
user=> (type/checked :even-point {:y 1})
:x must be present and non-nil
:y is not `even?`
nil
```

You can add more than one check per key:

```clojure
(global-type/named! :even-point [:x :y] {:x [#'even? #'pos?] :y [#'even? #'pos?]})

user=> (type/checked :even-point {:x -1, :y -2})
:x is not `even?`
:y is not `pos?`
nil
```

Note that because the `even?` check failed for `:x`, the `pos?` check was never performed.

### Custom error messages

Within a vector, you can replace a predicate with a vector of the form `[<pred> :message <fmt>]` to
provide a custom error message:

```clojure
(let [allowable [even? :message "%s is not an allowable number."]]
  (global-type/named! :allowable [:x :y] {:x [allowable] :y [allowable]}))

user=> (type/checked :allowable {:x 1, :y 2})
:x is not an allowable number.
nil
```

The message is formatted as with `format`. There can be two format
specifiers, which are both handed strings created with `pr-str`. The
first is the erroneous key. The second is the value of that key. So:

```clojure
(let [allowable [even? :message "%s should be even; %s isn't."]]
  (global-type/named! :ok [:x] {:x [allowable]}))

user=> (type/checked :ok {:x 1})
:x should be even; 1 isn't.
nil
```

For even more flexibility, the `:message` can be a function. It takes a map with two keys relevant here:

* `:path`: For non-nested maps like the ones we've seen above, this is a vector containing the
  key. Example: `[:x]`.

* `:value`: The erroneous value of that key. Example: `-1`.

Note that neither of these have been stringified by `pr-str`. Here's a silly example of printing the key with the square of the value:

```clojure
(letfn [(fmt [{:keys [path value]}]
         (format "%s should be a square root of %s." path (* value value)))]
  (global-type/named! :ok [:x] {:x [[even? :message fmt]]}))


user=> (type/checked :ok {:x 9})
[:x] should be a square root of 81.
nil
```

### Nested maps - TBD

As the previous section suggests, you can validate nested maps. Here
is how you require that a map have a `:point` field that contains an
`:x` and a `:y`:

```
(global-type/named! :point-container [[:point :x] [:point :y]])
```

### Optional keys - TBD

### Bouncer - TBD

### Predefined validations - TBD

### Defining validations - TBD


---------------------------------------------------

### Credits

I was inspired by Elm's typing for its [records](http://elm-lang.org/learn/Records.elm).

[Structural typing](http://en.wikipedia.org/wiki/Structural_type_system) is a respectable variant of typing, standing between name-based typing (which only is only indirectly about structure) and duck typing (which only cares about a particular field at a particular time). 

I first used type checking of this sort at
[GetSet](http://getset.com), though this API is better. (Sorry,
GetSet!)

[Bouncer](https://github.com/leonardoborges/bouncer) does the actual
validation.

### Contributing

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

