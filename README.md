# esfj

A Clojure library for defining JSR-330 Provider classes.

## Installation

Esfj is available on Clojars.  Add this `:dependency` to your Leiningen
`project.clj`:

```clj
[org.platypope/esfj "0.2.1"]
```

## Usage

The `esfj.providers` namespace exposes three public entry-points: the
base `provider` function for defining new anonymous Provider classes,
the `fn-provider` helper macro for defining providers via an `fn`-like
interface, and the `defprovider` helper macro for a `defn`-like
interface.

The `defprovider` macro supplies the most convenient interface.
Metadata `:tag`s on the argument parameters specify the types of the
Provider class constructor parameters.  The metadata `:tag` on the
parameter vector itself specifies the provided type.  A docstring and
further var metadata attributes may be added as per `defn`.  Example:

```clj
(require '[esfj.provider :refer [defprovider]])

(definterface SomeInterface)
(definterface SomeOtherInterface)

(defprovider example-provider
  "Example provider."
  ^SomeInterface [^SomeOtherInterface soi]
  (reify SomeInterface ...))
```

The resulting class will have a single, `@Inject` constructor
accepting the specified constructor argument types.  The supplied
`defprovider` body forms define a function over those constructor
arguments and should yield a value of the Provider return type.  By
default the constructor arguments are simply closed over, and the body
forms evaluated upon invocation of the Provider `.get` method.  If the
Provider name metadata or attribute map specifies `{:hof true}`, then
the Provider invokes the body forms during construction and expects
them to yield a zero-argument function to invoke during `.get`.

See the tests for more examples.

## License

Copyright © 2013 Marshall Bockrath-Vandegrift

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
