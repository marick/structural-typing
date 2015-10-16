Available via [clojars](https://clojars.org/marick/structural-typing) for Clojure 1.6+  
For lein: [marick/structural-typing "1.0-beta1"]    
License: [MIT](http://opensource.org/licenses/MIT)        
[API docs](http://marick.github.io/structural-typing/)       
[Wiki docs](https://github.com/marick/structural-typing/wiki)

[![Build Status](https://travis-ci.org/marick/structural-typing.png?branch=master)](https://travis-ci.org/marick/structural-typing)

# structural-typing

Many Clojure apps look something like this:

![Flow through a pipeline](https://github.com/marick/structural-typing/blob/master/doc/flow.png)


Data flows into them from some external source.
It's converted into a Clojure data structure: maps or vectors of
maps (1). The data is perhaps augmented by requesting related data from
other apps (2).

Thereafter, the data flows through a series of processing steps
(3). Each of them transforms structures into other structures. Some
steps might reduce a sequential structure into a map, or generate a
sequence from a map. But the most common transformation is to add,
remove, or change key-value pairs. Whatever the case, each step is
(should be) a relatively isolated, discrete, and
independently-understandable data transformation.

Finally (4), the result is passed on to some other app. It might be
the app that sent the data in the first place, or it might be another
app in a pipeline of microservices.

Conventional
([nominal](http://en.wikipedia.org/wiki/Nominal_type_system)) typing
is sometimes helpful for such a structure, but often it's not:

1. The structures coming into the app might have a
   fixed set of keys, 
   but they could easily also be larger structures that *contain* the
   fixed set of keys. Extra keys are to be preserved but not otherwise
   meddled with. As a trivial example, an app might process a stream
   of `Points`, requiring that each have `:x` and `:y` floating point
   coordinates. But it's perfectly acceptable for some points to also
   have a `:color`.

2. You might want to be more specific than "field `:color` is
   a String".  It might be important that `:color` encodes an RGB string.
   Type checks that are infeasible in static type systems are
   perfectly feasible at runtime; there's no reason to require types
   to be names when they can be arbitrary predicates.

4. Many transformation steps make only small changes to the structures
   they receive. When structures flow through a pipeline, giving the
   output of each stage its own type name is (1) annoying, and (2)
   likely to lead to really bad names. People reading those names will
   have to look to the source to understand them... kind of defeating
   the whole purpose of naming. For understandability, saying "step `N`
   produces a Type<sub>33</sub>" is less useful than "step `N` adds
   a `:color` field". 

This library is built to match the flow style of programming.

## A whirlwind tour

Type descriptions are stored in one or more [type-repos](https://github.com/marick/structural-typing/wiki/Glossary#type-repo). For these
examples, I'll use the predefined default global repo so that I don't
have to keep mentioning it all the time. (In production, I recommend a different [setup](https://github.com/marick/structural-typing/wiki/Recommended-setup).)

Let's say that a `:Point` is a map with `:x` and `:y` keys:

```clojure
user=> (use 'structural-typing.type 'structural-typing.global-type)
user=> (type! :Point (requires :x :y))
=> #TypeRepo[:Point]
```

The following shows how you check a value, and what the result of a successful check is:

```clojure
user=> (built-like :Point {:x 1 :y 2})
{:x 1, :y 2}
```

When given something that's not a proper `:Point`, `built-like`
prints a helpful message and returns `nil`: 

```clojure
user=> (built-like :Point {:x 1})
:y must exist and be non-nil
nil
```

(You can customize this behavior. For example, if you roll monadically,
you
[can have](https://github.com/marick/structural-typing/wiki/Using-the-Either-monad)
the success case produce a `Right` and the error case produce a
`Left`.)

This default behavior is useful for cleanly interrupting pipelines when errors occur:

```clojure
(some->> points
         (all-built-like :Point)
         (map color)
         (map embellish))
```

A map is allowed to have extra keys. For example, a colorful point is still a point:

```clojure
user=> (built-like :Point {:x 1, :y 2, :color "#DA70D6"})
=> {:x 1, :y 2, :color "#DA70D6"}
```

This is similar to the rules for [structural typing](https://en.wikipedia.org/wiki/Structural_typing) as used in [Elm](http://elm-lang.org/learn/Records.elm).

You might be surprised (I was) how often all you need to check at the
beginning of a pipeline is whether the input has all the required
keys. Inputs seem to either be right or wildly wrong; it's less often
the case that the `:x` and `:y` of a `:Point` are strings instead of
numbers. However, you can apply arbitrary predicates to any part of a
structure. Here, for example, are two equivalent ways of requiring that a
`:Point` have integer keys:

```clojure
(type! :Point (requires :x :y)
              {:x integer?, :y integer?})
(type! :Point {:x [required-path integer?]
               :y [required-path integer?]})
```

In either case, an error looks like this:

```clojure
user=> (built-like :Point {:x "one"})
:x should be `integer?`; it is `"one"`
:y must exist and be non-nil
=> nil
```

Feast your eyes on the "integer?". Pleasant error messages are
important. (And I'm smug about producing them without resorting to
macro-ology.)

(Terminology: although they're just plain predicates, I refer to
predicates on the right-hand side of type-description maps as
*check-preds*. That avoids ambiguity in some of the reference
documentation.)

Because error messages are important, you're allowed to name anonymous
check-preds. Here's a way of restricting the bounds of `:x` and `:y`:

```clojure
user=> (type! :Point (requires :x :y)
                     {:x (show-as "in bounds" #(and (>= % 0) (< % 1024)))
                      :y (show-as "in bounds" #(and (>= % 0) (< % 1024)))})
user=> (built-like :Point {:x -1, :y "five"})
:x should be `in bounds`; it is `-1`
:y should be `in bounds`; it is `"five"`
=> nil
```

Notice the second error message: when `"five"` was compared to `0`, the check-pred
threw a `ClassCastException`. That is interpreted as a false result. Otherwise,
the right-hand side of a type description would have to do error-checking that's not actually useful. 

Better than using anonymous functions, though, would be just defining a Clojure var:

```clojure
user=> (def within-bounds? #(and (>= % 0) (< % 1024)))
user=> (type! :Point (requires :x :y)
                     {:x within-bounds? :y within-bounds?})
user=> (built-like :Point {:x 1, :y -1})
:y should be `within-bounds?`; it is `-1`
=> nil
```

`:Point`'s definition is still a bit annoying, in that it doesn't
state clearly that `:x` and `:y` are of the same type. That can be
done like this:

```clojure
user=> (type! :Point {(each-of :x :y) [required-path within-bounds?]})
```

You've seen that the keys in a type-description map can be keywords or
a use of `each-of`. There are other possibilities, lumped under the term 
[paths](https://github.com/marick/structural-typing/wiki/Paths). Any path describes zero or more routes 
through a structure to individual substructures or *leaf values*.
For example, suppose you have a `:Figure` type
that contains many `:Points`. It can be described like this:

```clojure
(type! :Figure (requires :points :fill-color :line-color)
               {[:points ALL] (includes :Point)                 ; <<== interesting!
                (each-of :fill-color :line-color) rgb-string?})
```

The highlighted line above says that all values of the `:points` collection must satisfy the predicates that
define a 
`:Point`. (The name `includes` was chosen to remind you that the `:points`
are allowed to have additional fields.) An error looks like this:

```clojure
user=> (built-like :Figure {:fill-color "#DA70D6" :line-color "#DA70D6"
                            :points [{:x 1 :y -1} {:x 1 :y 2} {:x -1 :y 1}]})
[:points 0 :y] should be `within-bounds?`; it is `-1`
[:points 2 :x] should be `within-bounds?`; it is `-1`
=> nil
```

By using paths, you can describe a deeply nested structure as a flat
map whose keys are paths and whose values are collections of
check-preds.  But if you like type descriptions that are nested like the
types they describe, you can do that too:

```clojure
user=> (type! :X {:a even?
                  :b {:c1 odd?
                      :c2 string?}})
user=> (built-like :X {:a 1 :b {:c1 2, :c2 "foo"}})
:a should be `even?`; it is `1`
[:b :c1] should be `odd?`; it is `2`
=> nil
```

(However, I never actually got arround to supporting vector literals
in nested type descriptions - so this only works for maps. It could be added.)

Let's add another type to the type-repo, `:Colorful`. 

```clojure
user=> (type! :Colorful {:color [required-path rgb-string?]})
```

If you want to check that a value is a colorful point, you can check
against both types at the same time:

```clojure
user=> (built-like [:Colorful :Point] {:x 1})
:color must exist and be non-nil
:y must exist and be non-nil
=> nil
```

If, though, the idea of a colorful point is an important part of your
domain, it's easy to create one:

```clojure
user=> (type! :ColorfulPoint (includes :Colorful)
                             (includes :Point))
```

At the other extreme, there might only be one place where the
existence of a `:color` key matters. In that case, you can use an
unnamed bit of type description (analogous to an unnamed function):

```clojure
user=> (built-like [:Point (requires :color)] {:y 1})
:color must exist and be non-nil
:x must exist and be non-nil
=> nil
```

I've allowed snippets of type descriptions (like `(requires :color)`)
because it helps with a problem I have while reading pipelines of
processing stages: which stage does what? The following shows how
anonymous, partial type descriptions can help:

```clojure
(some->> patients        (all-built-like :Patient)
         add-history     (all-built-like (requires :history :appointments))
         flood-forward   (all-built-like {:schedule not-empty?})
         ...)
```

In this case, I only mention the `:Patient` type in the first check. I
don't use `:Patient` later because (1) I doubt later steps will
produce non-patients, so (2) reiterating it doesn't help a reader
understand the pipeline. If anything, the clutter hurts. I care more
that type descriptions are *informative*, even to someone reading
hastily, than that they're *complete*. If you care differently, you
can be more rigorous.

## Oh, by the way

You can check simple values, and you don't have to use a type name:

```clojure
user=> (built-like string? 5)
Value should be `string?`; it is `5`
=> nil
```


## For more details

See the [wiki](https://github.com/marick/structural-typing/wiki) for more methodical documentation, recommended setup, use with logging libraries and monads, and details on semantics. There is [API](http://marick.github.io/structural-typing/) documentation. It includes descriptions of [predefined predicates](http://marick.github.io/structural-typing/structural-typing.preds.html).

-------------------

# Back Matter

## Credits

I was inspired by Elm's typing for its [records](http://elm-lang.org/learn/Records.elm).

I first used type checking of this sort at
[GetSet](http://getset.com), though this version is way
better. (Sorry, GetSet!)

[Specter](https://github.com/nathanmarz/specter) does the work of
traversing structures, and Nathan Marz provided invaluable help with
Specter subtleties. [Potemkin](https://github.com/ztellman/potemkin)
gave me a function I couldn't write correctly myself. [Defprecated](https://github.com/alexander-yakushev/defprecated) came in handy as I flailed around in search of an API.


