App.module('Concepts', function(Concepts, App, Backbone, Marionette, $, _) {
    
    var style_binding = function(root, values) {
        var style_selector = root.find(".style-selector");

        Concepts.Concepts.find(function(c){
            return c.get('name') == 'styles';
        }).get('instances').each(function(i){
            var name = i.get('name');
            var icon = "<span class='icon style-icon'>&#xf040;</span>";
            var name_span = "<span class='style-name'>" + name + "</span>";
            var type = ":Style";
            var option = $("<option>").attr('value', name).html(icon + name_span + type);

            if(_.contains(values.styles, name)) {
                option.attr('selected', true);
            }

            style_selector.append(option);
        });

        style_selector.chosen();
    }

    var pages = new App.Models.Concept({
        name: 'pages',
        display_name: 'Pages',
        display_name_singular: 'Page',
        mode: 'xml',
        icon_code: 'f035',
        fields: ['url', 'styles', 'layout'],
        editor_js: ["/js/codemirror.js", "/js/mode/xml.js"],
        editor_css: ["/css/codemirror.css"],
        load: function(root, values) {
            root.find('.url').val(values.url);
            
            style_binding(root, values);

            //Layouts
            var layout_select = root.find('.layout');

            Concepts.Concepts.find(function(c){
                return c.get('name') == 'layouts';
            }).get('instances').each(function(i){
                var name = i.get('name');
                var option = $("<option>").attr('value', name).html(name);
                
                if(values.layout == name) {
                    option.attr('selected', true);
                }

                layout_select.append(option);
            });
            
            layout_select.chosen();
        },
        save: function(root) {
             return {
                url: root.find('.url').val(),
                styles: root.find('.style-selector').val(),
                layout: root.find('.layout').val()
            };
        }
    });

    var layouts = new App.Models.Concept({
        name: 'layouts',
        display_name: 'Layouts',
        display_name_singular: 'Layout',
        icon_code: "f121",
        fields: ['styles'],
        editor_js: ["/js/codemirror.js", "/js/mode/xml.js"],
        editor_css: ["/css/codemirror.css"],
        mode: 'xml',
        load: function(root, values) {
            style_binding(root, values);
        },
        save: function(root) {
            return {
                styles: _.map(root.find('.style'), function(s){
                    return $(s).val();
                })
            };
        },
    });

    var models = new App.Models.Concept({
        name: 'models',
        display_name: 'Models',
        display_name_singular: 'Model',
        icon_code: 'f0D7',
        fields: [],
        bodyless: true
    });

    var styles = new App.Models.Concept({
        name: 'styles',
        display_name: 'Styles',
        display_name_singular: 'Style',
        icon_code: 'f040',
        fields: [],
        mode: 'css'
    });
    
    var partials = new App.Models.Concept({
        name: 'partials',
        display_name: 'Partials',
        display_name_singular: 'Partial',
        icon_code: "f0d6",
        fields: ['styles'],
        mode: 'xml',
        load: function(root, values) {
            style_binding(root, values);
        },
        save: function(root) {
            return {
                styles: _.map(root.find('.style'), function(s){
                    return $(s).val();
                })
            };
        }
    });

    Concepts.loadInstances = function() {
        $.getJSON("/instances", {}, function(data){
            _.each(data, function(instances, concept_name){
                var concept = Concepts.Concepts.find(function(c){
                    return c.get('name') == concept_name;
                });

                var instance_coll = new App.Models.InstanceCollection();
                _.each(instances, function(vals){
                    instance_coll.push(new App.Models.Instance({
                        parent: concept,
                        name: vals.name,
                        body: vals.body,
                        values: vals.values
                    }));
                });

                concept.set({
                    instances: instance_coll
                });
            });
        });
    }

    Concepts.fetchFieldTmpls = function() {
        Concepts.Concepts.invoke('fetchFieldTmpl');
    }

    Concepts.Concepts = new App.Models.ConceptCollection([
        pages, 
        layouts, 
        styles, 
        models
        //partials
    ]);
});
