(ns spiral-clj.serializer
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]))

(defn serialize-instances [instances]
  (with-open [wrtr (io/writer "instances.json")]
    (.write wrtr (json/write-str instances))))
