(ns esfj.provider-test
  (:require [clojure.test :refer :all]
            [esfj.provider :refer [fn-provider defprovider]])
  (:import [clojure.lang IFn IDeref]
           [org.grouplens.grapht InjectorBuilder Module]))

(definterface ExampleMarker)

(deftest test-fn-provider
  (let [expected "yo-yo"
        bi (doto (InjectorBuilder. (into-array Module []))
             (-> (.bind ExampleMarker)
                 (.toProvider (fn-provider
                               ^ExampleMarker [^String s]
                               (reify ExampleMarker IDeref (deref [_] s)))))
             (-> (.at ExampleMarker)
                 (doto #_context
                   (-> (.bind String) (.to expected)))))
        actual (-> bi .build (.getInstance ExampleMarker) deref)]
    (is (= expected actual))))

(defprovider example-provider
  "An example provider class."
  {:tag `ExampleMarker}
  [^String s]
  (reify ExampleMarker IDeref (deref [_] s)))

(deftest test-defprovider
  (let [expected "woe"
        bi (doto (InjectorBuilder. (into-array Module []))
             (-> (.bind ExampleMarker) (.toProvider example-provider))
             (-> (.at ExampleMarker)
                 (doto #_context
                   (-> (.bind String) (.to expected)))))
        actual (-> bi .build (.getInstance ExampleMarker) deref)]
    (is (= expected actual))))

(defprovider ^:hof hof-provider
  "An example higher-order-function provider class."
  ^ExampleMarker [^String s]
  (partial deref (delay (reify ExampleMarker IDeref (deref [_] s)))))

(deftest test-defprovider-hof
  (let [expected "glorm"
        bi (doto (InjectorBuilder. (into-array Module []))
             (-> (.bind ExampleMarker) (.toProvider hof-provider))
             (-> (.at ExampleMarker)
                 (doto #_context
                   (-> (.bind String) (.to expected)))))
        actual (-> bi .build (.getInstance ExampleMarker) deref)]
    (is (= expected actual))))
