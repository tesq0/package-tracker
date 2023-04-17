(ns packagetracker.dpd-poland
  (:require [packagetracker.core :refer [get-tracking-info package-statuses]]
            [clj-http.client :as client]
            [clojure.string :refer [includes?]]))

(defn parse-response [response]
  (cond
    (includes? response "Przesyłka doręczona") (get package-statuses :delivered)
    (includes? response "Przesyłka niedoręczona") (get package-statuses :failed-attempt)
    (includes? response "Wydanie przesyłki do doręczenia") (get package-statuses :out-for-delivery)
    (includes? response "Przesyłka odebrana przez Kuriera") (get package-statuses :in-transit)
    :else (get package-statuses :pending)))

(defmethod get-tracking-info :dpd-poland [_ tracking-number]
  (-> (client/post "https://tracktrace.dpd.com.pl/findPackage"
                   {:form-params
                    {:q tracking-number
                     :typ 1}})
      parse-response))

