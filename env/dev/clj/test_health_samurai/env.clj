(ns test-health-samurai.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [test-health-samurai.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[test-health-samurai started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[test-health-samurai has shut down successfully]=-"))
   :middleware wrap-dev})
