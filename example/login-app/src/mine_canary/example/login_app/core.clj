(ns mine-canary.example.login-app.core
  (:require [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [compojure.core :as compojure :refer (GET POST ANY defroutes)]
            (compojure [handler :as handler]
                       [route :as route])
            [ring.util.response :as resp]
            [clojure.java.jdbc :as j]
            [hiccup.page :as h]
            [hiccup.element :as e])
  (:import [java.util Date]
           [java.sql Timestamp]))

(def oracle-db {:classname "oracle.jdbc.driver.OracleDriver"
                :subprotocol "oracle"
                :subname "thin:@localhost:1521/XE"
                :user "scott"
                :password "tiger"})

(def users (atom {"friend" {:username "friend"
                            :password (creds/hash-bcrypt "clojure")
                            :pin "1234" ;; only used by multi-factor
                            :roles #{::user}}
                  "friend-admin" {:username "friend-admin"
                                  :password (creds/hash-bcrypt "clojure")
                                  :pin "1234" ;; only used by multi-factor
                                  :roles #{::admin}}}))

(derive ::admin ::user)

(def login-form
  [:form.ui.form.segment.green {:method "POST" :action "/login"}
   [:div.field
    [:label "Username"]
    [:div.ui.left.icon.input
     [:i.user.icon]
     [:input {:type "text" :name "username" :placeholder "Name"}]]]
   [:div.field
    [:label "Passowrd"]
    [:div.ui.left.icon.input
     [:i.key.icon]
     [:input {:type "password" :name "password"}]]]
   [:div.field
    [:button.ui.green.button {:type "submit"} "Login"]]])

(defn layout [& body]
  (h/html5
   [:head
    (h/include-css "/css/semantic.min.css")]
   [:body
    [:div.main.container body]]))

(defroutes routes
  (GET "/" req
    (layout
     [:div.columns.small-12
      [:h2 "Interactive form authentication"]
      [:p "This app demonstrates typical username/password authentication, and a pinch of Friend's authorization capabilities."]
      [:h3 "Current Status " [:small "(this will change when you log in/out)"]]
      [:p (if-let [identity (friend/identity req)]
            (apply str "Logged in, with these roles: "
                   (-> identity friend/current-authentication :roles))
            "anonymous user")]
      login-form
      [:h3 "Authorization demos"]
      [:p "Each of these links require particular roles (or, any authentication) to access. "
       "If you're not authenticated, you will be redirected to a dedicated login page. "
       "If you're already authenticated, but do not meet the authorization requirements "
       "(e.g. you don't have the proper role), then you'll get an Unauthorized HTTP response."]
      [:ul [:li (e/link-to "role-user" "Requires the `user` role")]
       [:li (e/link-to "role-admin" "Requires the `admin` role")]
       [:li (e/link-to "requires-authentication"
                       "Requires any authentication, no specific role requirement")]]
      [:h3 "Logging out"]
      [:p (e/link-to "logout" "Click here to log out") "."]]))

  (GET "/login" req
    (layout
     [:div.columns.small-12 login-form]))

  (GET "/logout" req
    (friend/logout* (resp/redirect (str (:context req) "/"))))
  (GET "/requires-authentication" req
    (friend/authenticated "Thanks for authenticating!"))
  (route/resources "/"))

(def page (handler/site
           (friend/authenticate
            routes
            {:allow-anon? true
             :login-uri "/login"
             :default-landing-uri "/"
             :unauthorized-handler #(-> (h/html5 [:h2 "You do not have sufficient privileges to access " (:uri %)])
                                        resp/response
                                        (resp/status 401))
             :credential-fn (fn [params]
                              (let [ret (creds/bcrypt-credential-fn @users params)]
                                (j/insert! oracle-db "login_log"
                                            {:logined_at (Timestamp. (.getTime (Date.)))
                                             :account    (:username params)
                                             :ip_address "127.0.0.1"
                                             :success    (if (nil? ret) "0" "1")})
                                ret))
             :workflows [(workflows/interactive-form)]})))

