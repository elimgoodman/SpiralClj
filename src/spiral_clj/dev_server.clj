(ns spiral-clj.dev_server
  (:require [ring.adapter.jetty :as jetty])
  (:use [fleet :only [fleet]]))

(def my-instances (ref {}))

(defn get-instances [parent]
  (vals (parent @my-instances)))

(defn find-page-by-url [pages url]
  (first (filter #(= (:url %) url) pages)))

(defn get-layout-for-page [page]
  (let [layouts (get-instances :layouts)
        layout-for-page (:layout page)]
    (first (filter #(= (:name %) layout-for-page) layouts))))

(defn get-body-for-page [page]
  (let [page-content (:body page)
        layout (get-layout-for-page page)
        layout-body (:body layout)
        layout-tmpl (fleet [content] layout-body)]
    (str (layout-tmpl page-content))))

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


