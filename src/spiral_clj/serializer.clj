(ns spiral-clj.serializer
  (:require [clojure.data.json :as json]
            [clabango.parser :refer [render]]
            [clabango.tags :refer [deftemplatetag]]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:use [spiral-clj.dev_server :only [my-instances]]))

(defn serialize-obj [obj filename]
  (with-open [wrtr (io/writer filename)]
    (.write wrtr (json/write-str obj))))

(defn serialize-instances [instances]
  (serialize-obj instances "instances.json"))

(defn serialize-run-method [method instances]
  (do
    (serialize-obj method "run_method.json")))

;-------------------------------------------------------
;-------------------------------------------------------
;-------------------------------------------------------

(def tmpl-path "./test-project/")
(def target-path "./test-project-target/")
(def page-route-template "(defpage \"{{values.url}}\" [] (get-template \"{{name}}\"))")
(def page-file-template "{% inject layout as file with page-body=body %}")
(def layout-file-template "<cool>{{page-body}}</cool>")
(def default-file-template "{{body}}")

(defn re-match [pattern s]
  (second (re-find pattern s)))

(defn parse-assignment [assignment-str]
  (let [pieces (string/split assignment-str #"=")
        k (first pieces)
        v (second pieces)]
    {:key k
     :val v}))

(defn parse-assignments [assignment-str]
  (if (nil? assignment-str)
    nil 
    (let [assignments (string/split assignment-str #",")
          parsed (map parse-assignment assignments)]
          parsed)))

(defn parse-injection-args [s]
  (let [instance-name (re-match #"^([^\s]+)" s)
        view (re-match #"as ([^\s]+)" s)
        assignments (re-match #"with (.+)$" s)]
    {:instance-name (keyword instance-name)
     :view (keyword view)
     :assignments (parse-assignments assignments)}))

(defn get-view-tmpl [concept-name view]
  (case concept-name
    :layouts (case view
               :file layout-file-template)
    :styles (case view
              :file default-file-template)
    :pages (case view
             :route page-route-template
             :file page-file-template)))

(defn get-view-context [concept-name view instance]
  (case concept-name
    :pages (case view
             :file (let [layout-name (:layout (:values instance))
                         layouts (:layouts @my-instances)
                         layout (get-instance-by-name layouts layout-name)]
                     {:layout layout})
             instance)
    :layouts (case view
               :file {:page-body "beeeep"})
    instance))

(defn merge-assignments [context assignments instance]
  (let [assigned {}]
    (merge context assigned)))

(deftemplatetag "inject" [nodes context]
  (let [node (first nodes)
        args (:args node)
        arg-str (string/join " " args)
        parsed (parse-injection-args arg-str)
        instance-name (:instance-name parsed)
        view (:view parsed)
        assignments (:assignments parsed)
        instance ((keyword instance-name) context)
        concept-name (keyword (:parent instance))
        tmpl (get-view-tmpl concept-name view)
        context (get-view-context concept-name view instance)
        assigned (merge-assignments context assignments instance)
        rendered (string/trim (render tmpl context))]
    {:string rendered}))

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

(defn inject-files [tmpl-file]
  (let [path (.getPath tmpl-file)
        contents (slurp path)
        concept-to-inject (-> contents string/trim keyword)
        parent-path (.getParent tmpl-file)
        parent-target-path (make-target-path parent-path)
        extension (get-extension-for-concept concept-to-inject)]
    (doseq [instance (concept-to-inject @my-instances)]
      (let [instance-name (:name instance)
            filename (str instance-name "." extension)
            target-instance-path (str parent-target-path "/" filename)
            view :file
            tmpl (get-view-tmpl concept-to-inject view)
            context (get-view-context concept-to-inject view instance)]
        (spit target-instance-path (render tmpl context))))))

(defn inject-file [path target-path context]
  (let [contents (slurp path)
        injected (render contents context)]
    (spit target-path injected)))

(defn inject [instances]
  (let [context {:instances @my-instances}
        paths (get-paths tmpl-path)]
    (doseq [path paths]
      (let [target-path (make-target-path path)
            tmpl-file (io/file path)
            target-file (io/file target-path)]
        (cond (.isFile tmpl-file)
                (if (= (.getName tmpl-file) "_injections")
                  (inject-files tmpl-file)
                  (inject-file path target-path context))
              (.isDirectory tmpl-file)
                (.mkdirs target-file))))))
