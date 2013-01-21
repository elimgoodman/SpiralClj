(ns spiral-clj.server
  (:require [noir.server :as server])
  (:use [spiral-clj.dev_server :only [my-server create-server]]))

(server/load-views-ns 'spiral-clj.views)

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'spiral-clj})
    (dosync (ref-set my-server (create-server)))))

