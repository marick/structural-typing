Available via [clojars](https://clojars.org/marick/structural-typing) for Clojure 1.4+  
For lein: [marick/structural-typing "0.9.0"]     
License: [Unlicense](http://unlicense.org/) (public domain)        
[API docs](http://marick.github.io/structural-typing/)

[![Build Status](https://travis-ci.org/marick/structural-typing.png?branch=master)](https://travis-ci.org/marick/structural-typing)


This library provides an easy way to say "a given map must contain at
least these specific keys". For example, here's a global definition of the structural type `:point`:

```clojure
(require '[structural-typing.type :as type])

(type/named! :point [:x :y])
```

(There are also variants of these functions that don't mutate global state. See the [API docs](http://marick.github.io/structural-typing/).)


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

More generally, `set-failure-handler` can be set to any function that
takes a list of error messages. For example, I use
[Timbre](https://github.com/ptaoussanis/timbre) to control my logging, so my
type failures go to its `error` function.

Another alternative, if you have a monadic bent, is to use something like the Either monad. Here's an example with...

## Coercions

...

## Typing on more than keys

This library uses [bouncer](https://github.com/leonardoborges/bouncer)
under the hood. At some point, I'll expose more of that so that you
can, for example, declare that `:x` and `:y` must both be floats.

## Credits

I was inspired by Elm's typing for its [records](http://elm-lang.org/learn/Records.elm).

[Structural typing](http://en.wikipedia.org/wiki/Structural_type_system) is a respectable variant of typing, standing between name-based typing (which only is only indirectly about structure) and duck typing (which only cares about a particular field at a particular time). 

I first used type checking of this sort at
[GetSet](http://getset.com), though this API is better. (Sorry,
GetSet!)
