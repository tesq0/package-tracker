(ns packagetracker.trackhive
  (:require [packagetracker.core :refer [get-tracking-info]]
            [packagetracker.throttling :refer [mkthrottle]]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clj-http.client :as client]))

(def throttle (mkthrottle 10 1000))

(def http-get (throttle client/get))
(def http-post (throttle client/post))

(defn token [] (get (System/getenv) "TRACKHIVE_TOKEN"))

(defn headers []
  {:headers {"Content-Type" "application/json"
             "Authorization" (str "Bearer " (token))}})

(defmethod get-tracking-info :unknown [courier-slug tracking-number]
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

