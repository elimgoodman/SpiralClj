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

App.module('Sidebar', function(Sidebar, App, Backbone, Marionette, $, _) {
    
    Sidebar.ConceptView = Backbone.Marionette.Layout.extend({
        template: "#concept-list-tmpl",
        modelEvents: {
            'change': 'render'
        },
        regions: {
            instances: ".instances",
            newInstanceForm: '.new-instance-form'
        },
        ui: {
            instances: ".instances"
        },
        onRender: function() {
            this.instances.show(new Sidebar.InstanceListView({
                collection: this.model.get('instances')
            }));
        },
        events: {
            'click .add-instance-link': 'addInstance'
        },
        addInstance: function() {
            var v = new Sidebar.NewInstanceForm({
                parent: this.model,
                newInstanceForm: this.newInstanceForm
            });
            this.newInstanceForm.show(v);
        },
        className: 'concept',
        tagName: 'li'
    });

    Sidebar.NewInstanceForm = Backbone.Marionette.ItemView.extend({
        template: "#new-instance-form",
        ui: {
            name: 'input.name'
        },
        events: {
            'keyup input.name': 'maybeSubmit'
        },
        maybeSubmit: function(e) {
            if(e.which == 13) {
                var name = $(e.target).val();
                var instance = new App.Models.Instance({
                    parent: this.options.parent,
                    name: name
                });

                this.options.parent.get('instances').push(instance);
                this.options.newInstanceForm.close();
                App.Selections.Instance.set(instance);
            } else if(e.which == 27) {
                this.options.newInstanceForm.close();
            }
        },
        onRender: function() {
            this.ui.name.focus();
        }
    });

    Sidebar.ConceptListView = Backbone.Marionette.CollectionView.extend({
        itemView: Sidebar.ConceptView,
        tagName: 'ul'
    });

    Sidebar.InstanceView = Backbone.Marionette.ItemView.extend({
        template: "#instance-list-tmpl",
        tagName: 'div',
        className: 'instance',
        events: {
            'click .name': 'setSelection',
            'click .delete-link': 'deleteInstance'
        },
        setSelection: function() {
            App.Selections.Instance.set(this.model);
        },
        deleteInstance: function() {
            this.model.destroy();
        }
    });

    Sidebar.InstanceListView = Backbone.Marionette.CollectionView.extend({
        itemView: Sidebar.InstanceView
    });

    Sidebar.ActionLinks = Backbone.Marionette.View.extend({
        el: "#action-links",
        events: {
            'click #save-link': 'save'
        },
        save: function(e) {
            //if(S.CurrentInstance.get()) {
                //S.TheEditor.save();
            //}

            var data = {};

            App.Concepts.Concepts.each(function(c){
                data[c.get('name')] = c.get('instances').map(function(i){
                    return i.get('values');
                });
            });

            $.post("/save", {instances: data});

            e.preventDefault();
        }
    });
});

App.module('Selections', function(Selections, App, Backbone, Marionette, $, _) {
    var SelectionKeeper = function () {
        this.selected = null;
        this.prechange = null;
    };
    _.extend(SelectionKeeper.prototype, Backbone.Events, {
        set: function (selected) {
            if(this.selected && this.prechange) {
                this.prechange();
            }

            if(this.selected) {
                this.selected.set({
                    selected: false
                });
            }
            this.selected = selected;
            this.selected.set({
                selected: true
            });
            this.trigger('change');
        },
        get: function () {
            return this.selected;
        }
    });

    Selections.Instance = new SelectionKeeper();
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

    Editor.SelectionListener = Backbone.Marionette.Controller.extend({
        initialize: function() {
            App.Selections.Instance.bind('change', this.selectionChanged, this);
        },
        selectionChanged: function() {
            var v = new App.Editor.InstanceView({
                model: App.Selections.Instance.get()
            });
            App.editor.show(v);
        }
    });
});

App.addInitializer(function(options){
    App.Concepts.loadInstances();
    App.Concepts.fetchFieldTmpls();
    
    new App.Sidebar.ActionLinks();
    new App.Editor.SelectionListener();

    var v = new App.Sidebar.ConceptListView({
        collection: App.Concepts.Concepts
    });
    
    App.concept_list.show(v);
});

$(function() {
    App.start();
});
