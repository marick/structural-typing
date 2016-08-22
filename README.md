Available via [clojars](https://clojars.org/marick/structural-typing) for Clojure 1.7+  
For lein: [marick/structural-typing "2.0.5"]        
License: [MIT](http://opensource.org/licenses/MIT)        
[API docs](http://marick.github.io/structural-typing/)       
[Wiki docs](https://github.com/marick/structural-typing/wiki)

[![Build Status](https://travis-ci.org/marick/structural-typing.png?branch=master)](https://travis-ci.org/marick/structural-typing)

**[Semantic versioning](http://semver.org)**: Changes to the text of
the default error explanations will not trigger a major version
bump. Changes in the number of messages won't either, provided the new
set contains the same information. (In less-common cases, the library
says almost the same thing twice.)

**Updating to 2.0**: See the [change log](https://github.com/marick/structural-typing/blob/master/CHANGELOG.md). It is extremely unlikely that you'll have to change client code, unless you used functions that were explicitly deprecated.

# structural-typing

This library provides two services:

1. Good error messages when checking the correctness of structures
   (in, for example, tests).

2. A way to define
   [structural types](https://en.wikipedia.org/wiki/Structural_type_system)
   that are checked at runtime. Structural typing differs from the
   more common
   [nominal typing](https://en.wikipedia.org/wiki/Nominal_type_system)
   in that, for example, two records with different `defrecord` names
   are of the same type if they have the same keys.


## Good error messages are ever so helpful

The core function is `built-like`. When given a [type description](https://github.com/marick/structural-typing/wiki/Glossary#condensed-type-description) and a correct value, its return value is
the input value:

```clojure
user=> (use 'structural-typing.type)
user=> (built-like string? "foo")
"foo"
```

If the value is incorrect, an error message is printed to standard output and `nil` is returned:

```clojure
user=> (built-like string? :keyword)
Value should be `string?`; it is `:keyword`
=> nil
```

(A later section justifies the `nil` return. Also,
printing a message to standard output is almost never what you want in
production, so it can be overridden. It's useful for showing things in
the repl, though.)

Notice that I've declared the type to be "string" by using the Clojure
function `string?`. You can use any function. For example:

```clojure
(defn long? [x] (> (count x) 10))

user=> (built-like [long? string?] "foo")
Value should be `long?`; it is `"foo"`
=> nil
```

The library goes to considerable trouble to print function names
nicely. It also avoids unhelpful annoyances like `long?` blowing up
when given an integer:

```clojure
user=> (built-like [long? string?] 5)
Value should be `long?`; it is `5`
Value should be `string?`; it is `5`
=> nil
```

For better or worse, Clojure is one of those languages that treats `nil` as a member of
every type. By default, this library follows that convention:

```clojure
user=> (built-like string? nil)
=> nil
```

(It's the lack of an error message that distinguishes this `nil` result from a signal of an error.)

You can override this default if you like:

```clojure
user=> (built-like [string? reject-nil] nil)
The whole value should not be `nil`
=> nil
```

### Structures and paths

As shown above, `built-like` can be used with basic types like strings,
but that's unusual. The type more usually describes a collection. For example, here's a claim
that all values in a collection are even:

```clojure
user=> (built-like {[ALL] even?} [1 2 3 nil])
[0] should be `even?`; it is `1`
[2] should be `even?`; it is `3`
```

Notice that the `nil` was not rejected. To do that, you'd have to be explicit:

```clojure
user=> (built-like {[ALL] [even? reject-nil]} [1 2 3 nil])
[0] should be `even?`; it is `1`
[2] should be `even?`; it is `3`
[3] has a `nil` value
=> nil
```

Items (like `[ALL]`) in the left-hand (key) positions of the map are
[paths](https://github.com/marick/structural-typing/wiki/Glossary#path)
into the structure. For example, suppose each element of a collection
is a map. In that map, `:a` should have an even value, and `:b` should
have an odd value. The type description would have two paths:

```clojure
user=> (def my-type {[ALL :a] even?
                     [ALL :b] odd?})
user=> (built-like my-type [{:a 0 :b 0} {:a 1 :b 1}])
[0 :b] should be `odd?`; it is `0`
[1 :a] should be `even?`; it is `1`
=> nil
```

As before, `nil` values would be silently accepted. Moreover, missing
values are, too. (The justification for that decision is below.) For example:

```clojure
user=> (built-like my-type [{:a 0}])
=> [{:a 0}]    ;; how is this ok?! There's no :b!
```

`reject-nil` can be used in such a case, relying on the fact that `(:a {})` is `nil`:

```clojure
user=> (def my-type {[ALL :a] [even? reject-nil]
                     [ALL :b] [odd? reject-nil]})
user=> (built-like my-type [{:a 0}])
[0 :b] has a `nil` value
=> nil
```

However, the concept of "missing" is really different than "present
but `nil`", so you can more specifically reject missing values:

```clojure
user=> (def my-type {[ALL :a] [even? reject-missing]
                     [ALL :b] [odd? reject-missing]})
user=> (built-like my-type [{:a nil}]) 
[0 :b] does not exist
=> nil
```

Notice that `built-like` accepted the explicit `nil`. If you want to
reject both `nil` and missing values, use `required-path`:

```clojure
user=> (def my-type {[ALL :a] [even? required-path]
                     [ALL :b] [odd? required-path]})
user=> (built-like my-type [{:a nil}])
[0 :a] has a `nil` value
[0 :b] does not exist
=> nil
```

Note: You'll often want all or most paths to be required, and it would
be tedious and error-prone to type `required-path` on every right-hand
side in the type description. See [quickly requiring certain paths](https://github.com/marick/structural-typing#quickly-requiring-certain-paths) below, the wiki, and the API documentation.

### Sometimes I want to be specific about expected values

In testing, you'll often want to say not that a
value is a string, but that it's a *specific* string. That can be done
like this:

```clojure
user=> (require '[structural-typing.preds :as pred])
user=> (built-like {[:a :b] (pred/exactly 3)}
                   {:a {:b 4}})
[:a :b] should be exactly `3`; it is `4`
=> nil
```

You may not want exact comparison. There are a variety of useful
related functions in the `structural-typing.preds` namespace.  For
example, you can constrain the possible inputs to an API by insisting
that the version number match a regular expression:

```clojure
user=> (def version-check {[:metadata :version] (pred/matches #"1.?")})
user=> (built-like version-check {:metadata {:version "2.3"}
                                  :real-data 5})
[:metadata :version] should match #"1.?"; it is "2.3"
=> nil
```


#### A bit of path shorthand and a tad of terminology

In its
[canonical](https://github.com/marick/structural-typing/wiki/Glossary#canonical-type-description)
form, a type description is a map from paths to a vector of
"[checkers](https://github.com/marick/structural-typing/wiki/Glossary#checker)".
Here is a canonical description:

```clojure
{[:a] [even?]
 [:b] [odd?]
 [:c] [(pred/exactly 5)]}
```

However, you may have already noticed that you don't need the vector when you have a single checker:

```clojure
{[:a] even?
 [:b] odd?
 [:c] (pred/exactly 5)}
```

The same is true when the path has only one element:

```clojure
{:a even?
 :b odd?
 :c (pred/exactly 5)}
```

The above are two (possibly dubious) examples of my emphasis on
pleasing shorthand. Another example is motivated by the following:

```clojure
user=> (built-like {[] [string?]} "foo")
=> "foo"
```

The `[]` represents a path that stops short of descending
into the given value (`"foo"` in this case). Instead, the
checkers are applied to `"foo"` itself (the ["whole value"](https://github.com/marick/structural-typing/wiki/Glossary#whole-value)).

That looks a bit ugly, but you've already seen the abbreviated form:

```clojure
user=> (built-like string? "foo")
"foo"
```

I know which *I* prefer.

## The justification of some seemingly odd design choices

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

*Or*... the pipeline needs to be interrupted at some point because
something has gone wrong. Clojure's `some->` and `some->>` are useful
for this purpose, as they will short-circuit a pipeline if any step
produces a nil:

```clojure
user=> (some-> {:a {:b 1}}
               :a           ; produces {:b 1}
               :c           ; produces nil
               inc)         ; this is never reached
=> nil
```

This library adapts well to that style: check the result of each step
(most often at the end of pipeline stages that are functions). If the
value checks out, `built-like` will pass it along. Otherwise, it will
issue an out-of-band error message and return `nil` so that `some->`
will stop processing.

(If you prefer monads, you can [easily change](https://github.com/marick/structural-typing/wiki/Using-the-Either-monad) `built-like`'s behavior
so that it returns an Either value.)

Because of the way structures are transformed as they flow through the
pipeline, you often do not want to say "Structure X *must* contain
substructure Y". Instead, you want to say "*If* substructure Y has
been added, it should look like *this*". That's the reason why
`built-like` accepts missing or `nil` values unless otherwise instructed.
It's only at the
end of the pipeline that you want to append a further qualification
"... and all of these substructures are required."

Although not mentioned above, `built-like` will never complain about
extra structure. If you declare that a point must have integer `:x` and `:y`
coordinates, `built-like` will happily accept a map that satisfies that constraint but also
has a `:color` key. (See
[Elm's records](http://elm-lang.org/docs/records#record-types) for
more examples.)

That's it for paths, their traversals, and how oddly-shaped structures
are handled. But another peculiarity of this library is that
fundamental types are described with predicates (like `string?`)
instead of names (like [Schema's](https://github.com/Prismatic/schema)
`s/Str`). Why?

The use of names (so-called [nominal typing](https://en.wikipedia.org/wiki/Nominal_type_system))
restricts the language you can use to describe data. You can speak of a `:color` that
is a string, but not a string that specifically encodes an RGB-format
color value (as might be tested by an `rgb-string?` predicate). That's
because type systems have historically been targeted at *static*
(compile-time) analysis, and that's intractable with arbitrary
predicates. But it's perfectly fine for
runtime checking, which is why this library leans so heavily on predicates.
To us, values type-check if they satisfy predicates. Building up new or composite types means adding new predicates that values must satisfy.


## But names are kind of handy...

Indeed they are. If you have a `Point` type, it might be nice to name
it. One way would be to use `def`

```clojure
(def Point {:x [required-path integer?]
            :y [required-path integer?]})
```

Essentially, you're using a map from type names to type
descriptions. The type names are globally accessible because they're
vars stored in a namespace's `ns-publics` map.

That works. But, as it turns out, some kinds of shorthand type
descriptions are cleaner and clearer when we use a separate map of type names to
descriptions, one that isn't layered on top of a namespace. That's called a
[type-repo](https://github.com/marick/structural-typing/wiki/Glossary#type-repo).

A particular program can have many type repos, each using the
[recommended setup](https://github.com/marick/structural-typing/wiki/Recommended-setup) for ease of use. For repl samples like those on this page, it's more convenient to use the *global type repo*. It lets you give keyword or string names to the sort of descriptions you've already seen:

```clojure
user=> (use 'structural-typing.global-type)
user=> (type! :Point {:x [required-path integer?]
                      :y [required-path integer?]})
user=> (built-like :Point {:x 1.5})
:x should be `integer?`; it is `1.5`
:y does not exist
=> nil
```

Once you have named types, you'll want to combine them. Suppose we
have a separate `:Colorful` type like this:

```clojure
user=> (type! :Colorful {:color string?})
```

You can check to see whether a map is built like both a `:Point` and a `:Colorful`:

```clojure
user=> (built-like [:Colorful :Point] {:x 1, :color 1})
:color should be `string?`; it is `1`
:y does not exist
=> nil
```

On the other hand, if all you want to say is that some specific
`:Point` seen at one place in your code has a string `:color` value,
naming a new type might be excessive. So you can combine type names
and "anonymous" snippets of description:

```clojure
user=> (built-like [:Point
                    {:color string?}]   ; an anonymous addition to `:Point`
                   {:x 1, :color 1})
:color should be `string?`; it is `1`
:y does not exist
=> nil
```


If, though, the idea of a colorful point is an important part of your
domain, it's easy to create one:

```clojure
user=> (type! :ColorfulPoint (includes :Colorful)
                             (includes :Point))
user=> (built-like :ColorfulPoint {:x 1, :color 1})
:color should be `string?`; it is `1`
:y does not exist
=> nil
```

(The use of `includes` is intended to remind you that a
`:ColorfulPoint` is not limited to the fields named by the types. If
an incoming `:ColorfulPoint` includes a `:shape` key that your code is
uninterested in, that's perfectly fine: it'll just be passed along.)

Another way to combine existing types is to build up aggregate types. Let's suppose that
a `:Line` contains two points. That can be done like this:

```clojure
user=> (type! :Line {:head [required-path (includes :Point)]
                     :tail [required-path (includes :Point)]})
               
user=> (built-like :Line {:head {:x 1}})
:tail does not exist
[:head :y] does not exist
=> nil
```

There are other variant representations that the library
expands into canonical form. Frankly, some of them were more fun to
implement than they are useful, so I'll refer you to
[the wiki](https://github.com/marick/structural-typing/wiki) for
details.

But there's one worth mentioning here.

### Quickly requiring certain paths

You might be surprised (I was) by how often all you need to check at
the beginning of a pipeline is whether the input has all the required
keys. Inputs seem to either be right or wildly wrong. Either `:x` and
`:y` are both present and both integers, or they're missing
completely. A programmer who remembered to assign `x` and `y` values to points
probably did not suddenly decide strings would be a great way to represent them.

If you believe that, your type declarations might be no more than:

```clojure
user=> (type! :ColorfulPoint {:x required-path
                              :y required-path
                              :color required-path})
```

Awfully verbose for what you want to accomplish. This is better:

```clojure
(type! :ColorfulPoint (requires :x :y :color))
```

You might be more suspicious. You'll believe that `:x` and `:y` are valid (if present), but you'd like to check that `:color` actually satisfies `rgb-color?`. That's done like this:

```clojure
(type! :ColorfulPoint (requires :x :y :color)
                      {:color rgb-string?})
```

## Changing what happens when the checked value is incorrect

You can [log messages using a logging library](https://github.com/marick/structural-typing/wiki/Using-a-logging-library) instead of printing to standard output.

You can use a [monadic](https://github.com/marick/structural-typing/wiki/Using-the-Either-monad) (inline) style of error reporting.

You can easily [throw an exception](http://marick.github.io/structural-typing/structural-typing.type.html#var-throwing-error-handler) when an error is encountered.

## Similar libraries

[Schema](https://github.com/Prismatic/schema) and [Truss](https://github.com/ptaoussanis/truss). *At some point, there will be a link to a compare-and-contrast page.*

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

## Contributors

* Alessandro Andrioni
* Bahadir Cambel
* Brian Marick
* Devin Walters
