(ns esfj.provider-test
  (:require [clojure.test :refer :all]
            [esfj.provider :refer [provider fn-provider defprovider]])
  (:import [clojure.lang IFn IDeref]
           [org.grouplens.grapht InjectorBuilder Module]))

(definterface ExampleMarker)

(defn provider-actual
  [^String s ^Class p]
  (-> (doto (InjectorBuilder. (into-array Module []))
        (-> (.bind ExampleMarker) (.toProvider p))
        (-> (.at ExampleMarker) (.bind String) (.to s)))
      .build (.getInstance ExampleMarker) deref))

(deftest test-provider
  (let [expected "ding-ding"
        provider (provider
                  ExampleMarker [String]
                  (fn [s]
                    (constantly
                     (reify ExampleMarker
                       IDeref (deref [_] s)))))
        actual (provider-actual expected provider)]
    (is (= expected actual))))

(deftest test-fn-provider
  (let [expected "yo-yo"
        provider (fn-provider
                  ^ExampleMarker [^String s]
                  (reify ExampleMarker IDeref (deref [_] s)))
        actual (provider-actual expected provider)]
    (is (= expected actual))))

(defprovider example-provider
  "An example provider class."
  {:tag `ExampleMarker}
  [^String s]
  (reify ExampleMarker IDeref (deref [_] s)))

(deftest test-defprovider
  (let [expected "woe"
        actual (provider-actual expected example-provider)]
    (is (= expected actual))))

(defprovider ^:hof hof-provider
  "An example higher-order-function provider class."
  ^ExampleMarker [^String s]
  (partial deref (delay (reify ExampleMarker IDeref (deref [_] s)))))

(deftest test-defprovider-hof
  (let [expected "glorm"
        actual (provider-actual expected hof-provider)]
    (is (= expected actual))))
