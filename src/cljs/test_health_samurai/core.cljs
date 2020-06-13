(ns test-health-samurai.core)

(defn init! []
  (-> (.getElementById js/document "content")
      (.-innerHTML)
      (set! "Hello, World!")))
