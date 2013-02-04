(ns spiral-clj.dev_server
  (:require [ring.adapter.jetty :as jetty]
            [hiccup.core :as hiccup])
  (:use [fleet :only [fleet]]))

(def my-instances (ref {}))
(def my-run-method (ref ""))

(defn get-instances [parent]
  (vals (parent @my-instances)))

(defn find-page-by-url [pages url]
  (first (filter #(= (:url %) url) pages)))

(defn find-partial-by-name [partials name]
  (first (filter #(= (:name %) name) partials)))

(defn get-layout-for-page [page]
  (let [layouts (get-instances :layouts)
        layout-for-page (:layout page)]
    (first (filter #(= (:name %) layout-for-page) layouts))))

(defn hydrate-styles [style-names]
  (let [styles (get-instances :styles)]
    (filter (fn [style]
              (some #(= (:name style) %) style-names))
              styles)))

(defn get-styles [instance]
  (let [styles-for-instance (:styles instance)]
    (hydrate-styles styles-for-instance)))

(defn make-style-block [style]
  (let [body (:body style)]
    (hiccup/html [:style {:type "text/css"} body])))

(defn make-style-blocks [styles]
  (apply str (map make-style-block styles)))

(defn include-partial [partial-name]
  (let [partials (get-instances :partials)
        partial (find-partial-by-name partials partial-name)]
    (:body partial)))

(defn get-content-for-page [page]
  (let [body (:body page)
        body-tmpl (fleet [include] body)]
    (body-tmpl include-partial)))

(defn get-partial-names-in-body [body]
  (let [matches (re-seq #"<\(include \"([^\"]+)\"\)>" body)]
    (map second matches)))

(defn get-partials-in-page [page]
  (let [body (:body page)
        all-partials (get-instances :partials)
        partial-names (get-partial-names-in-body body)
        find (partial find-partial-by-name all-partials)]
    (map find partial-names)))

(defn get-style-names-from-partials [partials]
  (let [all-styles (map :styles partials)]
    (reduce conj all-styles)))

(defn get-body-for-page [page]
  (let [page-content (get-content-for-page page)
        partials-included (get-partials-in-page page)
        layout (get-layout-for-page page)
        layout-body (:body layout)
        layout-tmpl (fleet [content styles] layout-body)
        page-styles (get-styles page)
        layout-styles (get-styles layout)
        partial-style-names (get-style-names-from-partials partials-included)
        partial-styles (hydrate-styles partial-style-names)
        styles (distinct (concat page-styles layout-styles partial-styles))
        style-blocks (make-style-blocks styles)]
    (println partials-included)
    (str (layout-tmpl page-content style-blocks))))

(defn response-from-page [page]
  (let [page? (nil? page)
        status (if page? 404 200)
        body (if page? "Boo" (get-body-for-page page))]
    {:status status 
     :body body
     :headers {"Content-Type" "text/html"}
    }))

(defn handler [request]
  (let [uri (:uri request)
        page (find-page-by-url (get-instances :pages) uri)]
    (response-from-page page)))

(defn create-server []
  (jetty/run-jetty handler {:port 4000 :join? false}))

(def my-server (ref nil))


