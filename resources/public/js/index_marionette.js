var App = new Backbone.Marionette.Application();

App.addRegions({
    concept_list: "#concept-list",
    editor: "#editor"
});

App.module('Models', function(Models, App, Backbone, Marionette, $, _) {

    Models.Instance = Backbone.Model.extend({
        defaults: {
            values: {},
            name: "",
            body: ""
        },
        initialize: function() {
            var values = {};

            var concept = this.get('parent');
            var null_vals = {};
            _.each(concept.get('fields'), function(field){
                null_vals[field] = null;
            });

            var values = _.extend(null_vals, this.get('values'));

            this.set({values: values}, {silent: true});
        }
    });

    Models.Concept = Backbone.Model.extend({
        defaults: {
            editor_js: [],
            editor_css: [],
            css_rules: {}
        },
        initialize: function() {
            if(this.get('instances') == undefined) {
                this.set({
                    instances: new Models.InstanceCollection()
                }, {silent: true});
            }
        },
        fetchFieldTmpl: function() {
            var tmpl_selector = "#" + this.get('name') + "-editor";
            this.field_tmpl = _.template($(tmpl_selector).html());
        }
    });

    Models.InstanceCollection = Backbone.Collection.extend({
        model: Models.Instance
    });

    Models.ConceptCollection = Backbone.Collection.extend({
        model: Models.Concept
    });
});

App.module('ConceptList', function(ConceptList, App, Backbone, Marionette, $, _) {
    
    ConceptList.ConceptView = Backbone.Marionette.Layout.extend({
        template: "#concept-list-tmpl",
        modelEvents: {
            'change': 'render'
        },
        regions: {
            instances: ".instances"
        },
        onRender: function() {
            this.instances.show(new ConceptList.InstanceListView({
                collection: this.model.get('instances')
            }));
        },
        className: 'concept',
        tagName: 'li'
    });

    ConceptList.ConceptListView = Backbone.Marionette.CollectionView.extend({
        itemView: ConceptList.ConceptView
    });

    ConceptList.InstanceView = Backbone.Marionette.ItemView.extend({
        template: "#instance-list-tmpl",
        tagName: 'li',
        className: 'instance',
        events: {
            'click': 'showEditor'
        },
        showEditor: function() {
            var v = new App.Editor.InstanceView({
                model: this.model
            });
            App.editor.show(v);
        }
    });

    ConceptList.InstanceListView = Backbone.Marionette.CollectionView.extend({
        itemView: ConceptList.InstanceView
    });
});

App.module('Editor', function(Editor, App, Backbone, Marionette, $, _) {
    Editor.InstanceView = Backbone.Marionette.ItemView.extend({
        template: "#instance-editor-tmpl",
        ui: {
            fields: ".fields",
            body: ".body"
        },
        templateHelpers: {
            getFields: function() {
                var concept = this.parent;
                return concept.field_tmpl(this.values);
            }
        },
        events: {
            'click .toggle-fields': 'toggleFields'
        },
        toggleFields: function() {
            this.ui.fields.toggle();
        },
        onRender: function() {
            var concept = this.model.get('parent');
            concept.get('load')(this.$el, this.model.get('values'));

            this.body_cm = CodeMirror.fromTextArea(this.ui.body.get(0), {
                mode: concept.get('mode'),
                lineNumbers: true
            });

            var self = this;
            setTimeout(function(){self.body_cm.refresh();}, 20);
        }
    });
});

App.addInitializer(function(options){
    App.Concepts.loadInstances();
    App.Concepts.fetchFieldTmpls();

    var v = new App.ConceptList.ConceptListView({
        collection: App.Concepts.Concepts
    });
    
    App.concept_list.show(v);
});

$(function() {
    App.start();
});
