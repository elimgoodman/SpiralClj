(ns spiral-clj.serializer
  (:require [clojure.data.json :as json]
            [clabango.parser :refer [render render-file realize ast->groups groups->parsed]]
            [clabango.tags :refer [deftemplatetag]]
            [clabango.filters :refer [deftemplatefilter]]
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

(defn injector [name from to using]
  {:name name :from from :to to :using using})

(defn injection [kind values]
  {:kind kind :body values})

(defn file-injection [filename body]
  (injection :file {:filename filename :body body}))

(defn instance-injection [name body values]
  (injection :instance {:name name :body body :values values}))

(defn slugify [s]
  (let [replace-spaces (fn [s] (string/replace s \space \-))]
    (-> s string/lower-case replace-spaces)))

(def injectors [
  ;(injector "stylesheets" :styles :files (fn [instance]
    ;(let [filename (str (:name instance) ".css")
          ;body (:body instance)]
      ;(file-injection filename body))))

  (injector "all-objs-pages" :models :pages (fn [instance]
    (let [name-slug (-> instance :name slugify)
          name (str "All " (:name instance))
          body (str "<h1>" (:name instance) "</h1>")
          values {:url (str "/" name-slug) :layout "default"}]
      (instance-injection name body values))))
])

(defn instance-injections [instances injector]
  (let [from-concept (:from injector)
        using-fn (:using injector)
        instances-to-inject (from-concept instances)
        injections (map using-fn instances-to-inject)
        is-instance-injection (fn [i] (= (:kind i) :instance))
        instance-injections (filter is-instance-injection injections)
        new-instances (map #(:body %) instance-injections)]
    new-instances))

; This only does instance injections for the time being
(defn run-injectors [injectors instances]
  (let [injections (partial instance-injections instances)
        reducer (fn [m i] (assoc m (:to i) (injections i)))]
    (reduce reducer {} injectors)))

;-------------------------------------------------------
;-------------------------------------------------------
;-------------------------------------------------------

(deftemplatefilter "slugify" [node body arg]
  (slugify body))

(def tmpl-path "./test-project/")
(def target-path "./test-project-target/")

(def style-link-template "<link rel='stylesheet' type='text/css' href='/css/{{name}}.css' />")

(defn get-instance-by-name [instances name]
  (first (filter #(= (:name %) name) instances)))

(defn serialize-style [style]
  (render style-link-template style))

(defn serialize-styles [styles]
  (let [serialized (map serialize-style styles)]
    (string/join "\n" serialized)))

(defn inject-page-routes [instances]
  (let [route-file "src/test_project/views/welcome.clj"
        tmpl-file-path (str tmpl-path route-file)
        target-file-path (str target-path route-file)
        tmpl-str (slurp tmpl-file-path)
        rendered (render tmpl-str {:instances instances})]
    (spit target-file-path rendered)))

(defn render-page-body [page instances]
  (let [layout-name (-> page :values :layout)
        layouts (:layouts instances)
        layout (get-instance-by-name layouts layout-name)
        layout-body (:body layout)
        page-body (:body page)
        all-styles (:styles instances)
        style-names (-> page :values :styles)
        styles (map #(get-instance-by-name all-styles %) style-names)
        styles-serialized (serialize-styles styles)]
    (render layout-body {:content page-body :styles styles-serialized})))

(defn inject-template-files [instances]
  (let [pages (:pages instances)
        pages-dir "templates/"
        target-dir (str target-path pages-dir)]
    (doseq [page pages]
      (let [name-slug (-> page :name slugify)
            filename (str name-slug ".jinja")
            file-path (str target-dir filename)
            page-body (render-page-body page instances)]
        (spit file-path page-body)))))
  
(defn inject-styles [instances]
  (let [styles (:styles instances)
        styles-dir "resources/public/css/"
        target-dir (str target-path styles-dir)]
    (doseq [style styles]
      (let [filename (str (:name style) ".css")
            file-path (str target-dir filename)]
        (spit file-path (:body style))))))

(defn inject-model-files [instances]
  (let [template-dir "templates/"
        tmpl-dir (str tmpl-path template-dir)
        target-dir (str target-path template-dir)
        injection-file-path (str tmpl-dir "_injections")
        injection-file (slurp injection-file-path)
        injections (read-string injection-file)]
    (doseq [injection injections]
      (let [from-fn (-> injection :from eval)
            instances-to-use (from-fn instances)]
        (doseq [instance instances-to-use]
          (let [filename-fn (-> injection :filename eval)
                util {:slugify slugify} ;uhhhh
                filename (filename-fn instance util)
                body (:body injection)
                file-path (str target-dir filename)
                rendered (render body instance)]
            (spit file-path rendered)))))))

(defn merge-injected [instances injected]
  (merge-with concat instances injected))

(defn do-instance-injections [instances]
  (let [injected (run-injectors injectors instances)]
    (merge-injected instances injected)))

(defn inject [instances]
  (do
    (inject-page-routes instances)
    (inject-template-files instances)
    (inject-model-files instances)
    (inject-styles instances)))

;-------------------------------------------------------
;-------------------------------------------------------
;-------------------------------------------------------

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
             :route ""
             :file page-file-template)))

(defn get-additional-context [concept-name view instance]
  (case concept-name
    :pages (case view
             :file (let [layout-name (:layout (:values instance))
                         layouts (:layouts @my-instances)
                         layout (get-instance-by-name layouts layout-name)]
                     {:layout layout})
             {})
    ;:layouts (case view
               ;:file {:page-body "beeeep"})
    {}))

(defn get-view-context [concept-name view instance]
  (merge instance (get-additional-context concept-name view instance)))

(defn apply-assignment [context new-context assignment]
  (let [k (:key assignment)
        v (:val assignment)
        k-keyword (keyword k)
        v-keyword (keyword v)
        context-value (v-keyword context)]
    (assoc new-context k-keyword context-value)))

(defn merge-assignments [view-context context assignments]
  (if (empty? assignments)
    view-context
    (let [f (partial apply-assignment context)
          assigned (reduce f {} assignments)]
      (merge view-context assigned))))

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
        view-context (get-view-context concept-name view instance)
        assigned (merge-assignments view-context context assignments)
        rendered (string/trim (render tmpl assigned))]
    (if (empty? assignments) nil (println assigned))
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

;(defn inject [instances]
  ;(let [context {:instances @my-instances}
        ;paths (get-paths tmpl-path)]
    ;(doseq [path paths]
      ;(let [target-path (make-target-path path)
            ;tmpl-file (io/file path)
            ;target-file (io/file target-path)]
        ;(cond (.isFile tmpl-file)
                ;(if (= (.getName tmpl-file) "_injections")
                  ;(inject-files tmpl-file)
                  ;(inject-file path target-path context))
              ;(.isDirectory tmpl-file)
                ;(.mkdirs target-file))))))
