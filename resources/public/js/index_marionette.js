var App = new Backbone.Marionette.Application();

App.addRegions({
    concept_list: "#concept-list",
    editor: "#editor"
});

App.module('Util', function(Util, App, Backbone, Marionette, $, _) {
    Util.instanceHelpers = {
        getParentDisplayName: function() {
            return this.parent.get('display_name_singular');
        },
        getParentIcon: function() {
            return this.parent.get('icon_code');
        },
        parentHasFields: function() {
            return this.parent.get('fields').length > 0;
        }
    }
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
            css_rules: {},
            load: $.noop,
            save: $.noop,
            bodyless: false
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

    Models.Method = Backbone.Model.extend({
        defaults: {
            body: ""
        }
    });
});

App.module('RunMethod', function(RunMethod, App, Backbone, Marionette, $, _) {
    
    RunMethod.method = new App.Models.Method();

    RunMethod.Editor = Backbone.Marionette.ItemView.extend({
        template: "#run-method-editor-tmpl"
    });

    RunMethod.getEditorView = function() {
        return new RunMethod.Editor({
            model: RunMethod.method
        });
    }

    RunMethod.fetchRunMethod = function() {
        $.getJSON("/run_method", function(data){
            RunMethod.method = new App.Models.Method(data);
        });
    }
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

            this.$el.addClass(this.model.get('name'));
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
        },
        templateHelpers: App.Util.instanceHelpers
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
            App.Editor.save();

            var data = {};

            App.Concepts.Concepts.each(function(c){
                data[c.get('name')] = c.get('instances').map(function(i){
                    return {
                        name: i.get('name'),
                        body: i.get('body'),
                        values: i.get('values'),
                        parent: i.get('parent').get('name')
                    };
                });
            });
            
            var run_method = App.RunMethod.method.toJSON();
            
            var params = {
                instances: data,
                run_method: run_method
            };

            $.post("/save", {params: JSON.stringify(params)}, function(data){}, "json");

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
    
    var instanceHelpers = _.extend(App.Util.instanceHelpers, {
        getFields: function() {
            var concept = this.parent;
            return concept.field_tmpl(this.values);
        }
    });

    Editor.BodylessInstanceView = Backbone.Marionette.ItemView.extend({
        template: "#instance-editor-bodyless-tmpl",
        templateHelpers: instanceHelpers,
        className: 'bodyless'
    });

    Editor.InstanceView = Backbone.Marionette.ItemView.extend({
        template: "#instance-editor-tmpl",
        ui: {
            fields: ".fields",
            body: ".body"
        },
        templateHelpers: instanceHelpers,
        events: {
            'click .toggle-fields': 'toggleFields'
        },
        toggleFields: function() {
            this.ui.fields.slideToggle(100);
        }
    });

    Editor.getInstanceViewForModel = function(model) {
        var concept = model.get('parent');
        if(concept.get('bodyless')) {
            return new Editor.BodylessInstanceView({
                model: model
            });
        } else {
            return new Editor.InstanceView({
                model: model
            });
        }
    }

    Editor.SelectionListener = Backbone.Marionette.Controller.extend({
        initialize: function() {
            App.Selections.Instance.bind('change', this.selectionChanged, this);
        },
        selectionChanged: function() {
            var m = App.Selections.Instance.get();
            var v = App.Editor.getInstanceViewForModel(m);
            App.editor.show(v);
        }
    });

    Editor.resizeEditor = function() {
        var sidebar_width = $("#sidebar").outerWidth();
        var window_width = $(window).width();
        
        $("#editor").css({
            width: (window_width - sidebar_width) + "px"
        });
    }

    App.editor.on('show', function(view) {
        var instance = App.Selections.Instance.get();
        var concept = instance.get('parent');
        concept.get('load')(view.$el, instance.get('values'));

        var body = view.$('.body');
        body.val(instance.get('body'));

        if(!concept.get('bodyless')) {
            view.body_cm = CodeMirror.fromTextArea(body.get(0), {
                mode: concept.get('mode'),
                lineNumbers: true
            });
        }
    });

    App.Editor.save = function() {
        var instance = App.Selections.Instance.get();
        if(!instance) {return;}
        var concept = instance.get('parent');
        var values = concept.get('save')(App.editor.$el);

        instance.set({
            values: values,
            body: App.editor.currentView.body_cm.getValue()
        });
    }
});

App.addInitializer(function(options){
    App.Concepts.loadInstances();
    App.Concepts.fetchFieldTmpls();
    App.RunMethod.fetchRunMethod();

    new App.Sidebar.ActionLinks();
    new App.Editor.SelectionListener();

    var v = new App.Sidebar.ConceptListView({
        collection: App.Concepts.Concepts
    });

    App.Editor.resizeEditor();
    App.concept_list.show(v);
});

$(function() {
    App.start();
});
