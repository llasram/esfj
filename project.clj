(defproject esfj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [shady "0.1.1"]
                 [javax.inject "1"]
                 [org.grouplens.lenskit/lenskit-core "2.0.2"]]
  :aliases {"asmifier" ["run" "-m" ~(str "org.objectweb.asm.util"
                                         ".ASMifierClassVisitor")]}
  :profiles {:dev {:dependencies [[asm/asm "3.3"]
                                  [asm/asm-util "3.3"]]}})
