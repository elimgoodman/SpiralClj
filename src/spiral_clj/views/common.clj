(ns spiral-clj.views.common
  (:require [noir.server :as server]
            [ring.adapter.jetty :as jetty]
            [clojure.data.json :as j])
  (:use [noir.core :only [defpartial defpage]]
        [noir.response :only [json]]
        [spiral-clj.dev_server :only [my-instances my-run-method]]
        [spiral-clj.serializer :only [serialize-instances serialize-run-method inject]]
        [hiccup.page :only [include-css html5 include-js]]))

(defpage [:post "/save"] {:as body}
         (let [params-str (:params body)
               params (j/read-str params-str :key-fn keyword)
               instances (:instances params)
               run-method (:run_method params)]
           (serialize-instances instances)
           (inject instances)
           (serialize-run-method run-method instances)
           (dosync (ref-set my-instances instances))
           (dosync (ref-set my-run-method run-method))
           (json {:success true})))

(defpage "/instances" []
         (json @my-instances))

(defpage "/run_method" []
         (json @my-run-method))

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

(defn icon [code]
  (str "&#x" code ";"))

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
               (js "mode/clojure")
               (js "chosen")
               (js "marionette")
               ;(js "index")
               (js "index_marionette")
               (js "concepts")
               (include-css "/css/reset.css")
               (include-css "/css/chosen.css")
               (include-css "/css/codemirror.css")
               (include-css "/css/cm-blackboard.css")
               ;(less "/css/index.less")
               (less "/css/index_marionette.less")
               (js "less")]
              [:body
               [:div#sidebar
                [:ul#concept-list]
                [:div#action-links
                 [:div.action-link (ajax-link "save-link" (icon "F059"))]
                 [:div.action-link (ajax-link "run-method-link" (icon "F0B1"))]]]
               [:div#editor]
               ;[:div (ajax-link "add-instance-link" "Add")]
               ;[:div (ajax-link "save-link" "Save")]
               ;[:div (ajax-link "run-link" "Run")]
               ;[:div (ajax-link "stop-link" "Stop")]
              ]
              (js-template "concept-list-tmpl" 
                           [:div.header 
                            [:span.name (js-var "display_name")]
                            [:a {:href "#" :class "add-instance-link icon"} (icon "F14c")]]
                           [:div.instances]
                           [:div.new-instance-form])
              (js-template "instance-list-tmpl" 
                           [:span 
                            [:span.icon.parent-icon (icon (js-var "getParentIcon()"))]
                            [:a.name {:href "#"} (js-var "name")] ":" (js-var "getParentDisplayName()")]
                           [:a {:href "#" :class "delete-link"} "Delete"])
              (js-template "instance-editor-tmpl"
                           [:div.header 
                            [:span.icon.parent-icon (icon (js-var "getParentIcon()"))]
                            [:span [:span.name (js-var "name")] ":" (js-var "getParentDisplayName()")]
                            "<% if (parentHasFields()) { %>"
                              [:a {:href "#" :class "toggle-fields icon"} (icon "F01D")]
                             "<% } %>"
                            ]
                           [:div.fields (js-var "getFields()")]
                           [:textarea.body (js-var "body")])
              (js-template "run-method-editor-tmpl"
                           [:div.header 
                            [:span.icon.parent-icon (icon "F0B1")]
                            [:span [:span.name "RunMethod"] ":Clojure"]
                            ]
                           [:textarea.body (js-var "body")])
              (js-template "style-selector-tmpl" [:select.style])
              (js-template "pages-editor"
                           [:ul
                            [:li.field
                             [:label "URL:"][:input.field-input.url]]
                            [:li.field
                             [:label "Styles:"][:select.style-selector {:multiple true :data-placeholder "Add styles..."} ""]]
                            [:li.field
                             [:label "Layout:"][:select.layout "&nbsp;"]]])
              (js-template "layouts-editor"
                           [:ul
                            [:li.field
                             [:label "Styles:"][:select.style-selector {:multiple true :data-placeholder "Add styles..."} ""]]])
              (js-template "styles-editor" "&nbsp;")
              (js-template "new-instance-form" [:input.name])
              (js-template "partials-editor"
                           [:ul
                            [:li.field
                             [:label "Styles:"][:select.style-selector {:multiple true :data-placeholder "Add styles..."} ""]]])))
