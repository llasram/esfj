(defproject org.platypope/esfj "0.2.1"
  :description "A Clojure library for defining JSR-330 Provider classes."
  :url "http://github.com/llasram/esfj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :global-vars {*warn-on-reflection* true}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [javax.inject "1"]]
  :aliases {"asmifier" ["run" "-m" ~(str "org.objectweb.asm.util."
                                         "ASMifierClassVisitor")]}
  :profiles {:test {:resource-paths ["test-resources"]}
             :dev {:dependencies
                   [[org.grouplens.grapht/grapht "0.6.0"]
                    [asm/asm-util "3.3"]
                    [org.slf4j/slf4j-api "1.7.5"]
                    [org.slf4j/slf4j-log4j12 "1.7.5"]
                    [log4j "1.2.17"]]}})
