(ns test-project.views.welcome
  (:require [test-project.views.common :as common]
            [somnium.congomongo :as m])
  (:use [noir.core :only [defpage defpartial]]
        [noir.response :only [redirect]]))

(def conn
  (m/make-connection "test-db"))

(defn get-template [name]
  (slurp (str "./templates/" name ".jinja")))

(defn get-all [coll-name]
  (let [coll (keyword coll-name)]
    (m/with-mongo conn
      (m/fetch coll))))

(defpartial obj-li [obj]
  [:li (:name obj)])

(defpartial all-obj-page [obj-name objs]
  [:h1 obj-name]
  [:ul.objs
   (map obj-li objs)])

(defn create-obj [obj-name body]
  (let [coll (keyword obj-name)]
    (m/with-mongo conn
      (m/create-collection! coll)
      (m/insert! coll body))))


(defpage "/" [] (get-template "hello"))

(defpage "/foo" [] (get-template "beeo"))

(defpage "" [] (get-template "Space in here"))



(defpage "/foo" [] 
  (let [objs (get-all "foo")]
    (all-obj-page "foo" objs)))

(defpage "/foo/new" [] 
  (get-template "new-foo"))

(defpage [:post "/foo/create"] {:as body} 
  (do
    (create-obj "foo" body)
    (redirect "/foo")))

(defpage "/people" [] 
  (let [objs (get-all "people")]
    (all-obj-page "People" objs)))

(defpage "/people/new" [] 
  (get-template "new-people"))

(defpage [:post "/people/create"] {:as body} 
  (do
    (create-obj "people" body)
    (redirect "/people")))

