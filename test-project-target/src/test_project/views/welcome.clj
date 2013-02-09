(ns test-project.views.welcome
  (:require [test-project.views.common :as common])
  (:use [noir.core :only [defpage]]))

(defn get-template [name]
  (slurp (str "./templates/" name ".html")))

(defpage "/" [] (get-template "hello"))
(defpage "/foo" [] (get-template "beeo"))
