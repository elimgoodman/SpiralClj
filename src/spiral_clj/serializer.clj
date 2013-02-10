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
(def page-route-template "(defpage \"{{values.url}}\" [] (get-template \"{{name}}\"))")

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
    :styles "css"
    :pages "html"))

(defn get-instance-by-name [instances name]
  (first (filter #(= (:name %) name) instances)))

(defn get-instance-body [concept instance instances]
  (case concept 
    :styles (:body instance)
    :layouts (:body instance)
    :pages (let [layout-name (:layout (:values instance))
                 layouts (:layouts instances)
                 layout (get-instance-by-name layouts layout-name)
                 layout-body (get-instance-body :layouts layout instances)]
             layout-body)))

(defn inject-files [tmpl-file instances]
  (let [path (.getPath tmpl-file)
        contents (slurp path)
        concept-to-inject (-> contents string/trim keyword)
        parent-path (.getParent tmpl-file)
        parent-target-path (make-target-path parent-path)
        extension (get-extension-for-concept concept-to-inject)]
    (doseq [instance (concept-to-inject instances)]
      (let [instance-name (:name instance)
            filename (str instance-name "." extension)
            target-instance-path (str parent-target-path "/" filename)]
        (spit target-instance-path (get-instance-body concept-to-inject instance instances))))))

(defn inject-file [path target-path context]
  (let [contents (slurp path)
        injected (render contents context)]
    (spit target-path injected)))

(defn inject [instances]
  (let [context (make-context instances)
        paths (get-paths tmpl-path)]
    (doseq [path paths]
      (let [target-path (make-target-path path)
            tmpl-file (io/file path)
            target-file (io/file target-path)]
        (cond (.isFile tmpl-file)
                (if (= (.getName tmpl-file) "_injections")
                  (inject-files tmpl-file instances)
                  (inject-file path target-path context))
              (.isDirectory tmpl-file)
                (.mkdirs target-file))))))
