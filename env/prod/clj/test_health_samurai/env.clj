(ns test-health-samurai.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[test-health-samurai started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[test-health-samurai has shut down successfully]=-"))
   :middleware identity})
