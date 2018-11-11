(ns telemetry.streams
  (:use [clojure.tools.logging :only [info error]]
        [riemann.streams :only [call-rescue]]
        [riemann.time :only [unix-time once! next-tick]])
  (:require [riemann.folds :as folds]))

(defn part-time-carrying
  ([dt init-fn next-fn empty-fn add finish]
   (part-time-carrying dt init-fn next-fn empty-fn add (fn [state event]) finish))
  ([dt init-fn next-fn empty-fn add side-effects finish]
   (let [anchor (unix-time)
         state (atom {:window (init-fn)})

         ; Called every dt seconds to flush the window.
         tick (fn tick [self]
                (let [last-state (atom nil)
                      ; Swap out the current state
                      new-state (swap! state (fn [state]
                                               (let [next-window (next-fn (:window state) (:start state) (:end state))
                                                     end (next-tick anchor dt)]
                                                 (reset! last-state (assoc state :window next-window))
                                                 (if (empty-fn next-window)
                                                   {:window next-window}
                                                   {:window next-window :scheduled :first :start (- end dt) :end end}))))
                      s @last-state]
                  ; And finalize the last window
                  (when (not (empty-fn (:window s)))
                    (finish (:window s) (:start s) (:end s)))
                  (when (= :first (:scheduled new-state))
                    (once! (:end new-state)
                           (fn [] (self self))))))]

     (fn stream [event]
       ; Race to claim the first write to this window
       (let [state (swap! state (fn [state]
                                  ; Add the event to our window.
                                  (let [window (:window state)
                                        state (assoc state :window
                                                     (add window event))]
                                    (case (:scheduled state)
                                      ; We're the first ones here.
                                      nil (let [end (next-tick anchor dt)]
                                            (merge state
                                                   {:scheduled :first
                                                    :start (- end dt)
                                                    :end end}))

                                      ; Someone else just claimed
                                      :first (assoc state :scheduled :done)

                                      ; No change
                                      :done state))))]

         (when (= :first (:scheduled state))
           ; We were the first thread to update this window.
           (once! (:end state) (fn [] (tick tick))))

         ; Side effects
         (side-effects (:window state) event))))))

(defn sliding-window
  [flush-interval filter-fn & children]
  (part-time-carrying flush-interval
                      list
                      filter-fn
                      empty?
                      conj
                      (fn finish [window start end] (call-rescue (filter-fn window start end) children))))

(defn sliding-time-window
  "Every flush-interval seconds, forwards a list of events older than window-interal seconds.

	window-interval must be greater than or equal to flush-interval,
	though it's easier to use standard Riemann accumulators for the latter case."
  [window-interval flush-interval & children]

  (when (< window-interval flush-interval)
    (throw (IllegalArgumentException. "window-interval cannot be less than flush-interval")))

  (let [filter-window (fn [window start end]
                        (filter (fn [event]
                                  (let [diff (- end (:time event))]
                                    (< diff window-interval)))
                                window))]
    (apply sliding-window flush-interval filter-window children)))

(defn sliding-percentiles
  "Like standard riemann percentiles, but based on sliding windows."
  [window-interval flush-interval points & children]

  (sliding-time-window window-interval flush-interval
                       (fn [window]
                         (let [samples (folds/sorted-sample window points)]
                           (doseq [event samples] (call-rescue (assoc event :time (unix-time)) children))))))
