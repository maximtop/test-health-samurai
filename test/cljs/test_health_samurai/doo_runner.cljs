(ns test-health-samurai.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [test-health-samurai.core-test]))

(doo-tests 'test-health-samurai.core-test)

