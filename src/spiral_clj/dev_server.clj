(ns spiral-clj.dev_server
  (:require [ring.adapter.jetty :as jetty]
            [hiccup.core :as hiccup])
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

(defn get-styles-for-page [page]
  (let [styles (get-instances :styles)
        styles-for-page (:styles page)]
    (filter (fn [style]
              (some #(= (:name style) %) styles-for-page))
              styles)))

(defn make-style-block [style]
  (let [body (:body style)]
    (hiccup/html [:style {:type "text/css"} body])))

(defn make-style-blocks [styles]
  (apply str (map make-style-block styles)))

(defn get-body-for-page [page]
  (let [page-content (:body page)
        layout (get-layout-for-page page)
        layout-body (:body layout)
        layout-tmpl (fleet [content styles] layout-body)
        styles (get-styles-for-page page)
        style-blocks (make-style-blocks styles)]
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


