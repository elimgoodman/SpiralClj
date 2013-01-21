(ns spiral-clj.views.common
  (:require [noir.server :as server]
            [ring.adapter.jetty :as jetty])
  (:use [noir.core :only [defpartial defpage]]
        [noir.response :only [json]]
        [spiral-clj.dev_server :only [my-instances]]
        [hiccup.page :only [include-css html5 include-js]]))

(defpage [:post "/save"] {:as body}
         (let [instances (:instances body)]
           (dosync (ref-set my-instances instances))
           (json {:success true})))

(defpartial js [file]
            (include-js (str "/js/" file ".js")))

(defpartial ajax-link [id text]
            [:a {:id id :href "#"} text])

(defpartial js-template [id & content]
            [:script {:id id :type "text/html"} content])

(defn js-var [variable]
  (str "<%= " variable " %>"))

(defpage "/" []
         (html5
              [:head
               (js "jquery")
               (js "underscore")
               (js "backbone")
               (js "loader")
               (js "codemirror")
               (js "mode/xml")
               (js "index")
               (include-css "/css/codemirror.css")
               (include-css "/css/index.css")]
              [:body
               [:ul#concept-list]
               [:ul#instance-list]
               [:div#editor]
               [:div (ajax-link "add-instance-link" "Add")]
               [:div (ajax-link "save-link" "Save")]
               ;[:div (ajax-link "run-link" "Run")]
               ;[:div (ajax-link "stop-link" "Stop")]
              ]
              (js-template "concept-list-tmpl" (js-var "display_name"))
              (js-template "instance-list-tmpl" "inst")
              (js-template "pages-editor"
                           [:ul
                            [:li
                             [:label "URL: "][:input.url]]
                            [:li
                             [:label "Layout "][:select.layout]]
                            [:li
                             [:label "Body "][:textarea.body]]])
              (js-template "layouts-editor"
                           [:ul
                            [:li "Name: " [:input.name]]
                            [:li "Body: " [:textarea.body]]])
              (js-template "partials-editor"
                           [:ul
                            [:li "Name: " [:input.name]]
                            [:li "Body: " [:textarea.body]]])))
