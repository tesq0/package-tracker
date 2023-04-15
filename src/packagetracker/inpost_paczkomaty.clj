(ns packagetracker.inpost-paczkomaty
  (:require [packagetracker.core :refer [get-tracking-info package-statuses]]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [clojure.string :refer [includes?]]))

(defn parse-response [response]
  (case response
    "delivered" (get package-statuses :delivered)
    "undelivered" (get package-statuses :failed-attempt)
    "ready_to_pickup" (get package-statuses :out-for-delivery)
    "out_for_delivery" (get package-statuses :out-for-delivery)
    (get package-statuses :pending)))

(defmethod get-tracking-info :inpost-paczkomaty [_ tracking-number]
  (-> (client/get (str "https://api-shipx-pl.easypack24.net/v1/tracking/" tracking-number)
                  {:headers {"Content-Type" "application/json"}})
      :body
      (json/read-str :key-fn keyword)
      :status
      parse-response))

