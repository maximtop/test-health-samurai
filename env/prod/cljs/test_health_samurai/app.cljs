(ns test-health-samurai.app
  (:require [test-health-samurai.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
