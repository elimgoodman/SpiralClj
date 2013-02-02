$(function() {
    var S = window.S || {};
    
    S.CurrentConcept = new S.SelectionKeeper();
    S.CurrentInstance = new S.SelectionKeeper();
    
    S.CurrentInstance.prechange = function() {
        S.TheEditor.save();
    }

    S.MView = Backbone.View.extend({
        render: function() {
            this.$el.html(this.template(this.getTemplateContext()));
            this.$el.data('backbone-model', this.model);
            this.postRender();
            return this;
        },
        postRender: $.noop,
        getTemplateContext: function() {
            return this.model.toJSON();
        },
        initialize: function() {
            this.model.bind('change', this.render, this);
            this.template = _.template($("#" + this.template_id).html());
            this.postInit();
        },
        postInit: $.noop,
        template_id: null,
        setClassIf: function (if_cb, class_name) {
            if(if_cb()) {
                this.$el.addClass(class_name);
            } else {
                this.$el.removeClass(class_name);
            }
        },
        setSelectedClass: function () {
            var self = this;
            this.setClassIf(function () {
                return self.model.get('selected') == true;
            }, 'selected');
        }
    });


    S.Concepts = new S.ConceptCollection();

    S.ConceptListView = S.MView.extend({
        tagName: 'li',
        className: 'concept',
        template_id: 'concept-list-tmpl',
        events: {
            'click': 'selectConcept'
        },
        postRender: function() {
            this.setSelectedClass();
        },
        selectConcept: function() {
            S.CurrentConcept.set(this.model);
        }
    });

    S.InstanceListView = S.MView.extend({
        tagName: 'li',
        className: 'instance',
        template_id: 'instance-list-tmpl',
        events: {
            'click': 'selectInstance'
        },
        postRender: function() {
            this.setSelectedClass();
        },
        selectInstance: function() {
            S.CurrentInstance.set(this.model);
        },
        getTemplateContext: function() {
            var concept = this.model.get('parent');
            var id_field = concept.get('id_field');
            var values = this.model.get('values');
            
            var display_name = values[id_field];

            return _.extend(this.model.toJSON(), {
                display_name: display_name
            });
        }
    });

    S.ConceptList = Backbone.View.extend({
        el: $("#concept-list"),
        initialize: function() {
            this.render();
        },
        render: function() {
            this.$el.empty();
            S.Concepts.each(_.bind(this.renderOne, this));
        },
        renderOne: function(c) {
            var v = new S.ConceptListView({model: c});
            this.$el.append(v.render().el);
        }
    });

    S.InstanceList = Backbone.View.extend({
        el: $("#instance-list"),
        initialize: function() {
            S.CurrentConcept.bind('change', this.render, this);
        },
        render: function() {
            this.$el.empty();
            var concept = S.CurrentConcept.get();
            concept.get('instances').each(_.bind(this.renderOne, this));
        },
        renderOne: function(c) {
            var v = new S.InstanceListView({model: c});
            this.$el.append(v.render().el);
        }
    });

    S.AddInstanceLink = Backbone.View.extend({
        el: $("#add-instance-link"),
        events: {
            'click': 'addInstance'
        },
        addInstance: function(e) {
            if(e != undefined) {
                e.preventDefault();
            }

            var concept = S.CurrentConcept.get();
            var instance = new S.Instance({
                parent: concept
            });

            concept.get('instances').push(instance);
            S.TheInstanceList.renderOne(instance);
            S.CurrentInstance.set(instance);
            return instance;
        }
    });

    S.SaveLink = Backbone.View.extend({
        el: $("#save-link"),
        events: {
            'click': 'save'
        },
        save: function(e) {

        }
    });

    S.RunLink = Backbone.View.extend({
        el: $("#run-link"),
        events: {
            'click': 'run'
        },
        run: function(e) {
            $.post("/run");
        }
    });

    S.StopLink = Backbone.View.extend({
        el: $("#stop-link"),
        events: {
            'click': 'stop'
        },
        stop: function(e) {
            $.post("/stop");
        }
    });

    S.Editor = Backbone.View.extend({
        el: $("#editor"),
        initialize: function() {
            S.CurrentInstance.bind('change', this.render, this);
            this.loader = new Loader();
        },
        render: function() {
            var self = this;

            var instance = S.CurrentInstance.get();
            var concept = instance.get('parent');

            var editor_html = $(concept.editor_tmpl(instance.get('values')));

            this.$el.html(editor_html);
            this.loader.load({
                js: concept.get('editor_js'),
                css: concept.get('editor_css')
            }, function() {
                concept.get('load')(self.$el, instance.get('values'));
            });

            var css = this.serializeCSSRules(concept.get('css_rules'));
            this.appendCSSBlock(css);
        },
        serializeCSSRules: function(rules) {
            var output = "";
            _.each(rules, function(styles, selector) {
                output += selector + " {";
                
                _.each(styles, function(val, key){
                    output += key + ": " + val + ";";
                });

                output += "} ";
            });

            return output;
        },
        appendCSSBlock: function(css) {
            var block = $("<style>");
            block.html(css);
            this.$el.append(block);
        },
        save: function() {
            var instance = S.CurrentInstance.get();
            var concept = instance.get('parent');
            var values = concept.get('save')(this.$el);
            instance.set({values: values});
        }
    });

    //uhh
    var style_binding = function(root, values) {
        var style_selector = $($("#style-selector-tmpl").html());

        S.Concepts.find(function(c){
            return c.get('name') == 'styles';
        }).get('instances').each(function(i){
            var name = i.get('values')['name'];
            var option = $("<option>").attr('value', name).html(name);
            style_selector.append(option);
        });

        _.each(values.styles, function(s) {
            var selector = style_selector.clone();
            selector.val(s);
            var styles_li = $("<li>").append(selector);
            root.find(".styles").append(styles_li);
        });

        root.find(".add-style-link").click(function() {
            var styles_li = $("<li>").append(style_selector.clone());
            root.find(".styles").append(styles_li);
        });
    };


    S.Concepts.reset([pages, layouts, partials, styles]);

    S.TheConceptList = new S.ConceptList();
    S.TheInstanceList = new S.InstanceList();
    S.TheAddInstanceLink = new S.AddInstanceLink();
    S.TheRunLink = new S.RunLink();
    S.TheStopLink = new S.StopLink();
    S.TheSaveLink = new S.SaveLink();
    S.TheEditor = new S.Editor();
    
    S.CurrentConcept.set(S.Concepts.at(0));
    //instance = S.TheAddInstanceLink.addInstance();
    
    window.S = S;
});
