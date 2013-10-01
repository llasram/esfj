(ns esfj.grapht-test
  (:require [clojure.test :refer :all]
            [esfj.provider :refer [defprovider]]
            [esfj.grapht :refer [bind-provider]])
  (:import [clojure.lang IDeref]
           [org.grouplens.grapht InjectorBuilder Module]))

(definterface ExampleMarker)

(defprovider ExampleProvider
  ExampleMarker [])

(deftest test-defprovider
  (let [expected "yo-yo"
        bi (doto (InjectorBuilder. (into-array Module []))
             (bind-provider ExampleMarker ExampleProvider
                            (fn []
                              (constantly
                               (reify
                                 ExampleMarker
                                 IDeref (deref [_] expected))))))
        actual (-> bi .build (.getInstance ExampleMarker) deref)]
    (is (= expected actual))))
