(ns telemetry.folds)

(defn percentile
  [point]
  (fn [events]
    (let [sorted (sort-by :metric (filter :metric events))]
      (if (empty? sorted)
        '()
        (let [n (count sorted)
              idx (min (dec n)
                       (int (Math/floor (* n point))))]
          (nth sorted idx))))))
