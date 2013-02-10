(ns test-project.views.welcome
  (:require [test-project.views.common :as common])
  (:use [noir.core :only [defpage]]))

(defn get-template [name]
  (slurp (str "./templates/" name ".html")))

{% for page in instances.pages %}
{% inject page as route %}
{% endfor %}
