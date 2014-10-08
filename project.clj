(defproject net.unit8/mine-canary "0.1.0-SNAPSHOT"
  :description "Mine canary"
  :url "https://github.com/kawasima/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aot :all
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/data.fressian "0.2.0"]
                 [net.unit8/ulon-colon "0.2.0"]
                 [net.unit8/clj-flume-node "0.1.0" :exclusions [[io.netty/netty]]]
                 [org.apache.storm/storm-core "0.9.2-incubating" :exclusions[
                                               [ring/ring-devel]
                                               [io.netty/netty]]]]
  :main mine-canary.core

  :source-paths ["src/clj"])

