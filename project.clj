(defproject yolk-examples "0.1.0-SNAPSHOT"
  :description "Messing around with bacon.js and ClojureScript"
  :url "https://github.com/wilkes/yolk-examples"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [compojure "1.1.3"]
                 [hiccup "1.0.2"]
                 [jayq "2.3.0"]
                 [prismatic/dommy "0.0.1"]
                 [yolk "0.2.0-SNAPSHOT"]
                 [yolk-jquery "0.2.0-SNAPSHOT"]]
  :plugins [[lein-cljsbuild "0.3.0"]
            [lein-ring "0.8.2"]]
  :ring {:handler yolk-examples.server/app}
  :cljsbuild {:builds
              {:debug {:source-paths ["src"]
                       :compiler {:output-to "resources/public/js/cljs/main-debug.js"
                                  :optimizations :whitespace
                                  :pretty-print true}}}})