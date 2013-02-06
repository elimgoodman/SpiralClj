(ns test-project.views.welcome
  (:require [test-project.views.common :as common])
  (:use [noir.core :only [defpage]]))

(defpage "/welcome" []
         (common/layout
           [:p "Welcome to test-project"]))

{{page-routes}}
