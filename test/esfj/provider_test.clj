(ns esfj.provider-test
  (:require [clojure.test :refer :all]
            [esfj.provider :refer [defprovider]])
  (:import [clojure.lang IFn IDeref]
           [org.grouplens.grapht InjectorBuilder Module]
           [esfj.provider Factory]))

(definterface ExampleMarker)

(defprovider ExampleProvider
  ExampleMarker [String])

(deftest test-defprovider
  (let [expected "yo-yo"
        bi (doto (InjectorBuilder. (into-array Module []))
             (-> (.bind ExampleMarker) (.toProvider ExampleProvider))
             (-> (.at ExampleMarker)
                 (doto #_context
                   (-> (.bind String) (.to expected))
                   (-> (.bind Factory IFn)
                       (.to (fn [s]
                              (constantly
                               (reify
                                 ExampleMarker
                                 IDeref (deref [_] s)))))))))
        actual (-> bi .build (.getInstance ExampleMarker) deref)]
    (is (= expected actual))))
