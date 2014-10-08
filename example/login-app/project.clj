(defproject net.unit8.mine-canary.example/login-app "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [compojure "1.2.0"]
                 [hiccup "1.0.5"]
                 [ring/ring-core "1.3.1"]
                 [com.cemerick/friend "0.2.1"]
                 [com.oracle/ojdbc6 "11.2.0"]]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler mine-canary.example.login-app.core/page}

  :profiles {:dev
             {:dependencies [[javax.servlet/servlet-api "2.5"]]}})
