(ns esfj.grapht
  (:import [clojure.lang IFn]
           [org.grouplens.grapht Context]
           [esfj.provider Factory]))

(defn bind-provider
  "Add a binding to grapht Context `context` for class `desired` to be
provided by Provider class `provider` via function `factory`."
  [context desired provider factory]
  (doto context
    (-> (.bind desired) (.toProvider provider))
    (-> (.at desired)
        (doto #_context
          (-> (.bind Factory IFn) (.to factory))))))
