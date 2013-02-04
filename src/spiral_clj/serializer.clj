(ns spiral-clj.serializer
  (:require [clojure.data.json :as json]
            [clostache.parser :as clostache]
            [clojure.java.io :as io]))

(defn serialize-obj [obj filename]
  (with-open [wrtr (io/writer filename)]
    (.write wrtr (json/write-str obj))))

(defn serialize-instances [instances]
  (serialize-obj instances "instances.json"))

(defn serialize-run-method [method instances]
  (do
    (println (:pages instances))
    (println (clostache/render (:body method) instances))
    (serialize-obj method "run_method.json")))
