var App = new Backbone.Marionette.Application();

App.addRegions({
    concepts: "#concept-list",
    editor: "#editor"
});

App.start();


App.module('Models', function(Models, App, Backbone, Marionette, $, _) {

    Models.Instance = Backbone.Model.extend({
        defaults: {
            values: {}
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
            var tmpl_selector = "#" + this.get('name') + "-editor";
            this.editor_tmpl = _.template($(tmpl_selector).html());

            if(this.get('instances') == undefined) {
                this.set({
                    instances: new S.InstanceCollection()
                }, {silent: true});
            }
        }
    });

    Models.InstanceCollection = Backbone.Collection.extend({
        model: Models.Instance
    });

    Models.ConceptCollection = Backbone.Collection.extend({
        model: Models.Concept
    });
});
