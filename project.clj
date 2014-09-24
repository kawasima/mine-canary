(defproject net.unit8/mine-canary "0.1.0-SNAPSHOT"
  :description "Mine canary"
  :url "https://github.com/kawasima/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aot [mine-canary.core mine-canary.ulon-colon]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring "1.3.1"]
                 [com.google.guava/guava "18.0"]
                 [org.clojure/data.fressian "0.2.0"]
                 [net.unit8/ulon-colon "0.2.0-SNAPSHOT"]
                 [storm "0.9.0.1" :exclusions[[com.google.guava/guava]
                                              [ring/ring-devel]]]
                 [net.unit8/clj-flume-node "0.1.0"]

                 [compojure "1.1.9"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojurescript "0.0-2342"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.7.3"]
                 [sablono "0.2.22"]
                 [prismatic/om-tools "0.3.3"]]
  :main mine-canary.core

  :source-paths ["src/clj"]
  :plugins [[lein-ring "0.8.11"]
            [lein-cljsbuild "1.0.3"]]

  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]]}}
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/main.js"
                           :optimizations :simple}}]})










