(ns packagetracker.fedex
  (:require [packagetracker.core :refer [get-tracking-info package-statuses]]
            [clojure.data.json :as json]
            [clj-http.client :as client]))

(defn token [] (get (System/getenv) "FEDEX_TOKEN"))

(defn parse-response [response]
  (println response)
  (case response
    4 (get package-statuses :delivered)
    3 (get package-statuses :out-for-delivery)
    2 (get package-statuses :in-transit)
    (get package-statuses :pending)))

(defmethod get-tracking-info :fedex [_ tracking-number]
  (-> (client/get (str "https://api1.emea.fedex.com/fds2-tracking/trck-v1/info?trackingKey=" tracking-number)
                 {:headers {"apikey" (token)}})
      :body
      (json/read-str :key-fn keyword)
      :processStep
      int
      parse-response))

