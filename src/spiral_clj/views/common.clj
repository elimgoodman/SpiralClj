(ns spiral-clj.views.common
  (:require [noir.server :as server]
            [ring.adapter.jetty :as jetty])
  (:use [noir.core :only [defpartial defpage]]
        [noir.response :only [json]]
        [spiral-clj.dev_server :only [my-instances]]
        [spiral-clj.serializer :only [serialize-instances]]
        [hiccup.page :only [include-css html5 include-js]]))

(defpage [:post "/save"] {:as body}
         (let [instances (:instances body)]
           (serialize-instances instances)
           (dosync (ref-set my-instances instances))
           (json {:success true})))

(defpage "/instances" []
         (json @my-instances))

(defpartial js [file]
            (include-js (str "/js/" file ".js")))

(defpartial ajax-link [id text]
            [:a {:id id :href "#"} text])

(defpartial js-template [id & content]
            [:script {:id id :type "text/html"} content])

(defn js-var [variable]
  (str "<%= " variable " %>"))

(defn less [filename]
  [:link {:rel "stylesheet/less" :type "text/css" :href filename}])

(defpage "/" []
         (html5
              [:head
               (js "jquery")
               (js "underscore")
               (js "backbone")
               (js "loader")
               (js "codemirror")
               (js "mode/xml")
               (js "mode/css")
               (js "chosen")
               (js "index")
               (include-css "/css/reset.css")
               (include-css "/css/chosen.css")
               (include-css "/css/codemirror.css")
               (include-css "/css/cm-blackboard.css")
               (less "/css/index.less")
               (js "less")]
              [:body
               [:ul#concept-list.icon]
               [:ul#instance-list]
               [:div#editor]
               ;[:div (ajax-link "add-instance-link" "Add")]
               ;[:div (ajax-link "save-link" "Save")]
               ;[:div (ajax-link "run-link" "Run")]
               ;[:div (ajax-link "stop-link" "Stop")]
              ]
              (js-template "concept-list-tmpl" 
                           (str "&#x" (js-var "icon_code") ";"))
              (js-template "instance-list-tmpl" (js-var "display_name"))
              (js-template "style-selector-tmpl" [:select.style])
              (js-template "pages-editor"
                           [:div.header 
                            [:span.icon "&#xf035;"] [:span.name (js-var "url")] ":Page"]
                           [:ul
                            [:li.field
                             [:label "URL:"][:input.field-input.url]]
                            [:li.field
                             [:label "Styles:"][:select.style-selector {:multiple true :data-placeholder "Add styles..."} ""]]
                            [:li.field
                             [:label "Layout:"][:select.layout "&nbsp;"]]
                            [:li.field.body-field
                             [:div.label-container [:label "Body:"]][:textarea.body]]])
              (js-template "layouts-editor"
                           [:ul
                            [:li "Name: " [:input.name]]
                            [:li
                             [:label "Styles: "][:ul.styles]
                             [:a.add-style-link {:href "#"} "Add"]]
                            [:li "Body: " [:textarea.body]]])
              (js-template "styles-editor"
                           [:ul
                            [:li "Name: " [:input.name]]
                            [:li "Body: " [:textarea.body]]])
              (js-template "partials-editor"
                           [:ul
                            [:li "Name: " [:input.name]]
                            [:li
                             [:label "Styles: "][:ul.styles]]
                            [:li "Body: " [:textarea.body]]])))
