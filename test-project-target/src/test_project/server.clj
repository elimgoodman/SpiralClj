(ns test-project.server
  (:require [noir.server :as server]))

(server/load-views-ns 'test-project.views)

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "7000"))]
    (server/start port {:mode mode
                        :ns 'test-project})))

