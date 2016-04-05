(defproject clanhr/clj-librato "0.0.5"
  :description "Clojure interface to the Librato service"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "2.1.0"]
                 [cheshire "5.5.0"]]
  :profiles
  {:dev {:dependencies []}
   :test {:resource-paths ["resources" "test-resources"]}})
