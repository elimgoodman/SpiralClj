(ns spiral-clj.dev_server
  (:require [ring.adapter.jetty :as jetty]))

(def my-instances (ref {}))

(defn find-page-by-url [pages url]
  (first (filter #(= (:url %) url) pages)))

(defn response-from-page [page]
  (let [page? (nil? page)
        status (if page? 404 200)
        body (if page? "Boo" (:body page))]
    {:status status 
     :body body
     :headers {"Content-Type" "text/html"}
    }))

(defn get-instances [parent]
  (vals (parent @my-instances)))

(defn handler [request]
  (let [uri (:uri request)
        page (find-page-by-url (get-instances :pages) uri)]
    (response-from-page page)))

(defn create-server []
  (jetty/run-jetty handler {:port 4000 :join? false}))

(def my-server (ref nil))


