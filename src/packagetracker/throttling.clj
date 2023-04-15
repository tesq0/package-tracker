(ns packagetracker.throttling
  (:require [clojure.core.async :as async]))

(defn mkthrottle
  "Creates a throttling fuction that throttles the execution the input function within a time window, based on the allowed number of calls and the time period in seconds."
  [max-calls period-s]
  (let [call-count (atom 0)
        timer-chan (async/timeout (* period-s 1000))]
    (fn [f]
      (fn [& args]
        (when (<= @call-count max-calls)
          (async/go
            (when (async/alts! [timer-chan])
              (reset! call-count 0)))
          (swap! call-count inc)
          (apply f args))))))











