(set! *warn-on-reflection* true)
(ns packagetracker.main
  (:gen-class)
  (:require [clojure.core.async :as async]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :as jetty]
            [ring.util.codec :refer [url-encode]]))

(def rate-limit 10)
(def token-bucket (async/chan (async/dropping-buffer rate-limit)))

(defn start-token-bucket []
  (future
    (while true
      (async/>!! token-bucket true)
      (async/<!! (async/timeout 1000)))))

(defn throttle [f]
  (fn [& args]
    (async/<!! token-bucket)
    (apply f args)))

(def http-get (throttle client/get))
(def http-post (throttle client/post))

(def courier-list-url "https://api.trackinghive.com/couriers/list")

(def couriers-cache (atom ()))

(defn token [] (get (System/getenv) "PACKAGE_TRACKER_TOKEN"))

(defn headers []
  {:headers {"Content-Type" "application/json"
             "Authorization" (str "Bearer " (token))}})

(defn get-couriers []
  (or (and (not (empty? @couriers-cache)) @couriers-cache)
      (let [couriers-url "https://api.trackinghive.com/couriers/list"
            couriers-response (http-get couriers-url)
            couriers (as-> couriers-response v
                         (get v :body)
                         (json/read-str v :key-fn keyword)
                         (get v :data)
                         (map #(:slug %) v)
                         )]
        (reset! couriers-cache couriers)
        couriers)))

(defn get-tracking-info [courier-slug tracking-number]
  (let [existing-tracking-url (str "https://api.trackinghive.com/trackings?pageId=1&limit=1&filters="
                                   (json/write-str {:courier [courier-slug]})
                                   "&searchQuery=\"" tracking-number "\"")
        existing-tracking-response (http-get existing-tracking-url (headers))
        existing-tracking (-> existing-tracking-response
                                      :body
                                      (json/read-str :key-fn keyword)
                                      :data
                                      first)
        tracking-info (if existing-tracking
                        existing-tracking
                        (let [new-tracking-url "https://api.trackinghive.com/trackings"
                              new-tracking-body {:slug courier-slug
                                                 :tracking_number tracking-number}
                              new-tracking-response (http-post new-tracking-url (merge (headers)
                                                                                       {:body (json/write-str new-tracking-body)}))]
                          (-> new-tracking-response
                              :body
                              (json/read-str :key-fn keyword)
                              :data
                              )))]
    (get tracking-info :current_status)))

(defn valid-courier-slug? [slug]
  (some #(= slug %) (get-couriers)))  

(defn tracking-handler [{{courier-slug "courier_slug"
                          tracking-number "tracking_number"} :params}]
  (cond
    (not (valid-courier-slug? courier-slug)) {:status 400 :body (str
                                                                 "Invalid courier slug \"" courier-slug "\"" "\n"
                                                                 "Avaliable couriers: " (json/write-str (get-couriers)))}
    (not tracking-number) {:status 400 :body "tracking-number is required"}
    :else (let [tracking-info (get-tracking-info courier-slug tracking-number)]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body tracking-info})
    ))


(defn -main [& args]
  (start-token-bucket)
  (let [port (Integer/parseInt (or (get (System/getenv) "PORT") "3000"))]
    (jetty/run-jetty (wrap-params tracking-handler) {:port port})))
