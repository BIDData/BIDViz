function VegaLiteChart(id, name, size) {
    this.id = id;
    this.name = name;
    this.size = size;
    this.definition = {
        "data": {
            "values": []
        },
        "mark": "point",
        "encoding": {
            "x": {
                "field": "0", "type": "quantitative", "range": "width",
                "scale": {"zero": false}
            },
            "y": {"field": "1", "type": "quantitative"}
        }
    };
    var embedSpec = {
        mode: "vega-lite",  // Instruct Vega-Embed to use the Vega-Lite compiler
        spec: this.definition
        // You can add more vega-embed configuration properties here.
        // See https://github.com/vega/vega/wiki/Embed-Vega-Web-Components#configuration-propeties for more information.
    };
    this.data = [];
    // Embed the visualization in the container with id `vis`
    vg.embed('#' + id, embedSpec, function (error, result) {
    });
}

VegaLiteChart.prototype = new BaseViz("VegaLiteChart",VegaLiteChart)

VegaLiteChart.prototype.addPoint = function (ipass, sizes, values) {
    this.data.push([ipass, Number(values[0])]);
    this.definition.data.values = this.data.slice(this.data.length - 20);
    var embedSpec = {
        mode: "vega-lite",  // Instruct Vega-Embed to use the Vega-Lite compiler
        spec: this.definition
    };
    vg.embed('#' + this.id, embedSpec, function (error, result) {
        console.log(error, result);
    });
}


