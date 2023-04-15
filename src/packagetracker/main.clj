(ns packagetracker.main
  (:gen-class)
  (:require [packagetracker.core :refer [supported-couriers get-tracking-info]]
            [packagetracker.fedex]
            [packagetracker.trackhive]
            [packagetracker.dpd-poland]
            [packagetracker.inpost-paczkomaty]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :as jetty]))

(defn valid-courier-slug? [slug]
  (some #(= slug %) supported-couriers))

(defn tracking-handler [{{courier-slug "courier_slug"
                          tracking-number "tracking_number"} :params}]
  (cond
    (not (valid-courier-slug? courier-slug))
    {:status 400 :body (str
                        "Invalid courier slug \"" courier-slug "\"" "\n"
                        "Avaliable couriers: " (json/write-str supported-couriers))}
    (not tracking-number) {:status 400 :body "tracking-number is required"}
    :else (let [tracking-info (get-tracking-info courier-slug tracking-number)]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body tracking-info})))

(defn -main [& args]
  (let [port (Integer/parseInt (or (get (System/getenv) "PORT") "3000"))]
    (jetty/run-jetty (wrap-params tracking-handler) {:port port})))
