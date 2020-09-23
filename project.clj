(defproject hal "0.1.0-SNAPSHOT"
  :description A Clojure symmetrical encryption and tokenization library.
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.purefn/kurosawa.core "2.1.10"]
                 [joda-time "2.9.7"]]
  
  :profiles
  {:dev {:dependencies [[org.clojure/test.check "0.9.0"]
                        [org.purefn/kurosawa.core "2.1.10"]
                        [org.purefn/kurosawa.aws "2.1.10"]]}
   :provided {:dependencies [[org.purefn/kurosawa.core "2.1.10"]
                             [org.purefn/kurosawa.aws "2.1.10"]]}}
  :aot :all)
