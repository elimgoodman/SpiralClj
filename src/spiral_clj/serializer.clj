(ns spiral-clj.serializer
  (:require [clojure.data.json :as json]
            [clabango.parser :refer [render]]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn serialize-obj [obj filename]
  (with-open [wrtr (io/writer filename)]
    (.write wrtr (json/write-str obj))))

(defn serialize-instances [instances]
  (serialize-obj instances "instances.json"))

(defn serialize-run-method [method instances]
  (do
    (serialize-obj method "run_method.json")))

(def tmpl-path "./test-project/")
(def target-path "./test-project-target/")
(def page-route-template "(defpage \"{{values.url}}\" [])")

(defn copy-file [src-path dest-path]
  (io/copy (io/file src-path) (io/file dest-path)))

(defn make-page-routes [pages]
  (string/join "\n" (map #(render page-route-template %) pages)))

(defn make-context [instances]
  {:page-routes (make-page-routes (:pages instances))})

(defn get-paths [root]
  (let [f (io/file root)
        fseq (file-seq f)]
    (map #(.getPath %) fseq)))

(defn make-target-path [path]
  (string/replace path tmpl-path target-path))

(defn get-extension-for-concept [concept]
  (case concept 
    :styles "css"))

(defn inject [instances]
  (let [context (make-context instances)
        paths (get-paths tmpl-path)]
    (doseq [path paths]
      (let [target-path (make-target-path path)
            tmpl-file (io/file path)
            target-file (io/file target-path)]
        (cond (= (.getName tmpl-file) "_injections")
                (let [contents (slurp path)
                      concept-to-inject (-> contents string/trim keyword)
                      parent-path (.getParent tmpl-file)
                      parent-target-path (make-target-path parent-path)
                      extension (get-extension-for-concept concept-to-inject)]
                  (doseq [instance (concept-to-inject instances)]
                    (let [target-instance-path (str parent-target-path "/" (:name instance) "." extension)]
                      (println target-instance-path)
                      (spit target-instance-path (:body instance)))))
              (.isFile tmpl-file)
                (let [contents (slurp path)
                      injected (render contents context)]
                  (spit target-path injected))
              (.isDirectory tmpl-file)
                (.mkdirs target-file))))))
