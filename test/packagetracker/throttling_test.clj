(ns packagetracker.throttling-test
  (:require [clojure.test :refer :all]
            [packagetracker.throttling :refer [mkthrottle]]))

(defn test-operation [x]
  (println "Performing test operation with:" x)
  (+ x 1))

(defn count-calls [calls-performed num-calls seconds]
  (let [throttle (mkthrottle num-calls seconds)
        throttled-fn (throttle test-operation)
        start-time (System/currentTimeMillis)]
    (dotimes [i calls-performed]
      (throttled-fn i))
    (-> (System/currentTimeMillis)
        (- start-time)
        (/ 1000)
        int)))

(deftest test-throttle
  (let [total-calls 20
        allowed-calls 5
        period 1]
    (testing "Throttle macro allows only the specified number of calls per second"
      (is (<= (count-calls total-calls allowed-calls period)
              allowed-calls)))))
