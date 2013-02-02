App.module('Concepts', function(Concepts, App, Backbone, Marionette, $, _) {

    var pages = new App.Models.Concept({
        name: 'pages',
        display_name: 'Pages',
        icon_code: 'f035',
        id_field: 'url',
        fields: ['url', 'body', 'styles', 'layout'],
        editor_js: ["/js/codemirror.js", "/js/mode/xml.js"],
        editor_css: ["/css/codemirror.css"],
        load: function(root, values) {
            root.find('.url').val(values.url);

            var body = root.find('.body');
            body.val(values.body);
            
            var style_selector = $(".style-selector");

            Concepts.Concepts.find(function(c){
                return c.get('name') == 'styles';
            }).get('instances').each(function(i){
                var name = i.get('values')['name'];
                var icon = "<span class='icon style-icon'>&#xf040;</span>";
                var name = "<span class='style-name'>" + name + "</span>";
                var type = ":Style";
                var option = $("<option>").attr('value', name).html(icon + name + type);
                style_selector.append(option);
            });

            style_selector.chosen();

            //Layouts
            var layout_select = root.find('.layout');

            Concepts.Concepts.find(function(c){
                return c.get('name') == 'layouts';
            }).get('instances').each(function(i){
                var name = i.get('values')['name'];
                var option = $("<option>").attr('value', name).html(name);
                
                if(values.layout == name) {
                    option.attr('selected', true);
                }

                layout_select.append(option);
            });
            
            layout_select.chosen();

            //FIXME: Setting attributes on 'this' probably isn't great...
            this.cm = CodeMirror.fromTextArea(body.get(0), {
                mode: 'xml',
                lineNumbers: true
            });
        },
        save: function(root) {
            return {
                url: root.find('.url').val(),
                body: this.cm.getValue(),
                styles: _.map(root.find('.style'), function(s){
                    return $(s).val();
                }),
                layout: root.find('.layout').val()
            };
        }
    });

    var layouts = new App.Models.Concept({
        name: 'layouts',
        display_name: 'Layouts',
        icon_code: "f121",
        id_field: 'name',
        fields: ['name', 'styles', 'body'],
        editor_js: ["/js/codemirror.js", "/js/mode/xml.js"],
        editor_css: ["/css/codemirror.css"],
        load: function(root, values) {

            //style_binding(root, values);

            root.find('.name').val(values.name);

            var body = root.find('.body');
            body.val(values.body);

            this.cm = CodeMirror.fromTextArea(body.get(0), {
                mode: 'xml',
                lineNumbers: true
            });
        },
        save: function(root) {
            return {
                name: root.find('.name').val(),
                body: this.cm.getValue(),
                styles: _.map(root.find('.style'), function(s){
                    return $(s).val();
                })
            };
        },
    });

    var styles = new App.Models.Concept({
        name: 'styles',
        display_name: 'Styles',
        icon_code: 'f040',
        id_field: 'name',
        fields: ['name', 'body'],
        load: function(root, values) {
            root.find('.name').val(values.name);

            var body = root.find('.body');
            body.val(values.body);

            this.cm = CodeMirror.fromTextArea(body.get(0), {
                mode: 'css',
                lineNumbers: true
            });
        },
        save: function(root) {
            return {
                name: root.find('.name').val(),
                body: this.cm.getValue(),
            };
        },
    });
    
    var partials = new App.Models.Concept({
        name: 'partials',
        display_name: 'Partials',
        icon_code: "f0d6",
        id_field: 'name',
        fields: ['name', 'body', 'styles'],
        load: function(root, values) {
            //style_binding(root, values);

            root.find('.name').val(values.name);

            var body = root.find('.body');
            body.val(values.body);

            this.cm = CodeMirror.fromTextArea(body.get(0), {
                mode: 'xml',
                lineNumbers: true
            });
        },
        save: function(root) {
            return {
                name: root.find('.name').val(),
                body: this.cm.getValue(),
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
                        name: "Some name",
                        body: "Foo bar",
                        values: vals
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
        partials
    ]);
});
