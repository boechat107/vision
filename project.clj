(defproject vision "1.0.0-SNAPSHOT"
  :description "OpenCV wrapper for Clojure."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.incubator "0.1.0"]
                 [org.clojars.nakkaya/jna "3.2.7"]
                 [seesaw "1.4.3"]
                 [opencv "2.4.4"]
                 ]
  :native-path "native"
  :jvm-opts ["-Djna.library.path=resources/lib/"])
