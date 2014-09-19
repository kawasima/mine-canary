(defproject net.unit8/mine-canary "0.1.0-SNAPSHOT"
  :description "Mine canary"
  :url "https://github.com/kawasima/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aot :all
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.fressian "0.2.0"]
                 [net.unit8/ulon-colon "0.2.0-SNAPSHOT"]
                 [storm "0.9.0.1"]
                 [net.unit8/clj-flume-node "0.1.0"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [com.oracle/ojdbc6 "11.2.0.2.0"]]
  :main mine-canary.core)










