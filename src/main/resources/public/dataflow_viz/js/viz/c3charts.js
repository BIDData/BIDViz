function C3LineChart(id, names, size) {
    if (typeof(names) == "string")
        names = [names]
    this.names = names;
    console.log(id,names)
    this.id = id;
    this.minValue = 1000000000
    this.definition = {
        bindto: '#' + this.id,
        data: {
            x: 'x',
            columns:[ ['x'], ['y']]
        },
        zoom: {
            enabled: false
        }
    };
//    this.chart = c3.generate(this.definition);
//    return
    this.chart = null
    this.init = function(lower_bound) {
        var axes = {}
        axes[name] = "y2"
        var inf = -10000
        while (Math.floor(lower_bound-Math.abs(lower_bound)-10)<inf)
            inf*=10
        this.chart = c3.generate({
                    bindto: '#' + this.id,
                    data: {
                        columns: names.map(function(d){return [d].concat(d3.range(100).map(function(){return inf}))}),
                        axes: axes
                    },
                    point: {
                        show: false
                    },
                    zoom: {
                            enabled: false
                    },
                    axis: {
                        y: {
                            show:false
                        },
                        y2: {
                            show: true,
                            tick:{
                                format: function(d){return d3.round(d,5)}
                            }
                        },
                        x: {
                            tick: {
                                count:5,
                                format: function (x) { return parseInt((x-99)*1); }
                            }
                        }
                    },
                    transition: {
                         duration: 0
                    },
                    tooltip:{
                        format:{
                            value: function (value, ratio, id) {
                                return (value===inf)? "-INF" : d3.round(value,5)
                            }
                        }
                    }

                });
    }    
}

C3LineChart.prototype = new BaseViz("C3LineChart",C3LineChart)

C3LineChart.prototype.addPoint = function (ipass, sizes, values) {
/*    this.definition.data.columns[0].push(ipass);
    this.definition.data.columns[1].push(values[0]);
    this.chart.load(this.definition.data);
    return;*/
    var v = values.map(function(d){return parseFloat(d)*2})
    console.log(v)
    var value = d3.min(v)
    if (this.chart == null)
        this.init(value)
    if (Math.floor(value) < this.minValue) {
        this.minValue = Math.floor(value)
        this.chart.axis.min({y2:this.minValue})
    }
    var data = []
    for(var i=0;i<this.names.length;i++)
        data.push([this.names[i],v[i]])
    this.chart.flow({
            columns: data,
            length: 1,
            duration: 0 
        })
}
