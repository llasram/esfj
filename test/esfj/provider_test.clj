(ns esfj.provider-test
  (:require [clojure.test :refer :all]
            [esfj.provider :refer [fn-provider]])
  (:import [clojure.lang IFn IDeref]
           [org.grouplens.grapht InjectorBuilder Module]
           [esfj.provider Factory]))

(definterface ExampleMarker)

(deftest test-fn-provider
  (let [expected "yo-yo"
        bi (doto (InjectorBuilder. (into-array Module []))
             (-> (.bind ExampleMarker)
                 (.toProvider (fn-provider ^ExampleMarker [^String s]
                                (reify
                                  ExampleMarker
                                  IDeref (deref [_] s)))))
             (-> (.at ExampleMarker)
                 (doto #_context
                   (-> (.bind String) (.to expected)))))
        actual (-> bi .build (.getInstance ExampleMarker) deref)]
    (is (= expected actual))))
