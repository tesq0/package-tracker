(ns packagetracker.core)

(def supported-couriers ["dpd-poland"
                         "fedex"
                         "inpost-paczkomaty"
                         
                         "gls"
                         "dhl-poland"])

(def package-statuses {:pending "Pending"
                      :in-transit "InTransit"
                      :out-for-delivery "OutForDelivery"
                      :failed-attempt "FailedAttempt"
                      :delivered "Delivered"})

(defn delivery-service [service & args]
  (case service
    "fedex" :fedex
    "dpd-poland" :dpd-poland
    "inpost-paczkomaty" :inpost-paczkomaty
    :unknown))

(defmulti get-tracking-info delivery-service)

