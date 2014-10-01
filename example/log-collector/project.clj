(defproject net.unit8.mine-canary/log-collector "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [net.unit8/clj-flume-node "0.1.0"]
                 [net.unit8/ulon-colon "0.2.0-SNAPSHOT"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [com.oracle/ojdbc6 "11.2.0"]]
  :main mine-canary.example.log-collector.core)
