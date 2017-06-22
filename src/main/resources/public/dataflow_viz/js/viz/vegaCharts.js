function VegaLiteChart(id, name, config) {
    this.id = id;
    this.name = name;
    this.definition = {
        '$schema': 'https://vega.github.io/schema/vega-lite/v2.json',
        "data": {
            "name": "table"
        },
        "mark": "line",
        "encoding": {
            "x": {
                "field": "x", "type": "quantitative", "range": "width",
                "scale": {"zero": false}
            },
            "y": {"field": "y", "type": "quantitative"},
            'color': { 'field': 'category', 'type': 'nominal' }            
        }
    };
    if ("config" in config) {
        this.dataNames = config["config"]["data"]
        Object.assign(config["config"],this.definition)
        this.definition = config["config"]        
    }
    console.log(this.definition)

    var embedSpec = {
        mode: "vega-lite",  // Instruct Vega-Embed to use the Vega-Lite compiler
        config: {}
        // You can add more vega-embed configuration properties here.
        // See https://github.com/vega/vega/wiki/Embed-Vega-Web-Components#configuration-propeties for more information.
    };
    // Embed the visualization in the container with id `vis`
    this_tmp = this
    vega.embed('#' + id, this.definition,embedSpec, function (error, result) {
        d3.select("#" + id).select(".vega-actions").remove()
        this_tmp.result = result
        exports = result
    });
}

VegaLiteChart.prototype = new BaseViz("VegaLiteChart",VegaLiteChart)

VegaLiteChart.prototype.addPoint = function (ipass, sizes, values) {
//    this.data.push([ipass, Number(values[0])]);
//    this.definition.data.values = this.data.slice(this.data.length - 20);
    var dataNames = this.dataNames
    var getName = function(i){if (dataNames!=undefined) return dataNames[i];else return i}
    var changeSet = vega.changeset().insert(values.map(function(d,i){
                                                            return {x:ipass, y:Number(d),category:getName(i)}
                                                       }))
    this.result.view.change('table', changeSet).run();
    
    /*var embedSpec = {
        mode: "vega-lite",  // Instruct Vega-Embed to use the Vega-Lite compiler
        spec: this.definition
    };
    vg.embed('#' + this.id, embedSpec, function (error, result) {
        console.log(error, result);
    });*/
}


