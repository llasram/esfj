# esfj

A Clojure library for defining JSR-330 Provider classes.

## Installation

Esfj is available on Clojars.  Add this `:dependency` to your Leiningen
`project.clj`:

```clj
[org.platypope/esfj "0.1.1"]
```

## Usage

Define providers via the `esfj.provider/defprovider` macro.  It takes
three arguments: the name for the new class (automatically
package-scoped to the current namespace), the type produced by the
class instances (return type of the `.get` method), and the types of
arguments to the class constructor.  Example:

```clj
(require '[esfj.provider :refer [defprovider]])

(definterface SomeInterface)
(definterface SomeOtherInterface)

(defprovider ExampleProvider
  SomeInterface [SomeOtherInterface])
```

The resulting class will have a single constructor accepting an `IFn`
followed by the provided types.  The initial `IFn` parameter will be
annotated with the `esfj.provider.Factory` qualifier annotation.  At
class construction time, the provider invokes this (inject-able)
function with the remaining constructor argument.  The function should
return a zero-argument function providing the implementation for the
Provider `.get` method.

The `esfj.grapht/bind-provider` function provides a helper for binding
esfj-generated Providers in LensKit grapht configurations.  Example:

```clj
(bind-provider conf SomeInterface ExampleProvider
               (comp constantly some-value))
```

## License

Copyright © 2013 Marshall Bockrath-Vandegrift

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
