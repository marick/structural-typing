### An iffy vector feature

I'm not sure if this notation is a good idea, but I offer it for your consideration. Let's return to `:Point`:

```clojure
(type! :Point
       [requires :x :y]
       {:x integer? :y integer?})
```

Suppose we want to define a `:Line` type that has two points and a required color. Here's one way:

```clojure
(type! :Line
       [:start :end :color]
       {:color string?
        :start (includes :Point)
        :end (includes :Point)})
```

To my mind, there's a difference between `:start` (or `:end`) and `:color`. With the former, we're saying we're requiring something we already know about. With the latter, we're constraining a new field. It seems perhaps that the "old news" should be confined to the vector. We can do that by adding the `:Point` definition to the end of a path:

```clojure
(type! :Line
       (requires [:start (includes :Point)]
                 [:end (includes :Point)]
                 :color)
       {:color string?})
```

A map-terminated path inside a vector-style condensed description splits into two condensed descriptions. The clause `(requires [<path-elt>... <map>])` expands into these two:

```clojure
(requires [<path-elt>...])
{[<path-elt>..] <map>}
```

The case for this notation is perhaps a bit more compelling when defining a colorful `:Figure` made up of a collection of points:

```
(type! :Figure 
       (requires :color
                 [:points ALL (includes :Point)])
       {:color string?})
```

Frankly, there's a case to be made to forget all the vector description nonsense and just use maps throughout:

```clojure
(type! :Figure
       {:color [required-key string?]
        [:points ALL] [required-key (includes :Point)]})
```

You pays your money, and you takes your chance.
