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

{% for page in instances.pages %}
(defpage "{{page.values.url}}" [] (get-template "{{page.name}}"))
{% endfor %}

{% for model in instances.models %}
(defpage "/{{model.name|slugify}}" [] 
  (let [objs (get-all "{{model.name|slugify}}")]
    (all-obj-page "{{model.name}}" objs)))

(defpage "/{{model.name|slugify}}/new" [] 
  (get-template "new-{{model.name|slugify}}"))

(defpage [:post "/{{model.name|slugify}}/create"] {:as body} 
  (do
    (create-obj "{{model.name|slugify}}" body)
    (redirect "/{{model.name|slugify}}")))
{% endfor %}
