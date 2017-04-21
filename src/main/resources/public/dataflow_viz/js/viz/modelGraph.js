(function(exports){
    
    function createTooltip(d) {
        var res = "layerName: " + d.name + "<br/>"
        if (d.imodel>=0){
            res += "imodel: " + d.imodel + "<br/>"
            if (d.modelName != "") res += "modelName: " + d.modelName + "<br/>"
            res += "modelDim: " + d.modelDim + "<br/>"
            
        }
        res += "inputDim: " + d.inputDim + "<br/>"
        res += "outputDim: " + d.outputDim + "<br/>"
        var internal = d.internalLayers.map(function(l){if (l.modelName != "") return l.modelName; else return l.name})
        if (internal.length>0) res += "internalLayers: " + internal.join(" ") + "<br/>"
        return res;
    }
    
    function sequential(div,data) {
        div.append("div").append("tspan").html(data.model)
        var svg = div.append("div").append("svg").attr("width",600).attr("height",40*data.layers.length+50)
        var color = d3.scale.category20();
        var g = svg.selectAll("rect").data(data.layers).enter()
            .append("g")
        g.append("rect").attr("x",50)
            .attr("y",function(d,i){return i*40})
            .attr("width",200).attr("height",30)
            .attr("fill",function(d){return color(d.name)})
        g.append("text").attr("x",100)
            .attr("y",function(d,i){return i*40+25})
            .html(function(d){return d.name})
        
        var tooltip = d3.select("body").append("div")
            .attr("class", "tooltip")
            .style("opacity", 0);

        g.on("mouseover", function(d,i) {
            tooltip.transition()
                .duration(200)
                .style("opacity", .9);
            var text = createTooltip(d)
            tooltip.html(text)
                .style("left", (d3.event.pageX) + "px")
                .style("top", (d3.event.pageY - 50) + "px")
            tooltip.style("height",text.split("<br/>").length*15)
            d3.select(this).style("opacity", .7)
            })
        .on("mouseout", function(d) {
            tooltip.transition()
                .duration(500)
                .style("opacity", 0)
            d3.select(this).style("opacity", 1)
        });
    }
    
    function seqToSeq(div,data,_width,_height,_padding){
        div.append("div").append("tspan").html(data.model+"<br/>")
        var width = _width || 200
        var height = _height || 30
        var padding = _padding ||10
        var leftPadding = 50 
        var svg = div.append("div").append("svg")
            .attr("width",(width + padding) * (data.inwidth + data.outwidth)+leftPadding)
            .attr("height",(height + padding)*(data.height + 4)+padding)
        var cnt = 0
        for(var j=0;j<data.inwidth;j++)
            for(var i=0;i<data.height + 2;i++){
                data.layers[cnt].row = i
                data.layers[cnt].col = j;
                cnt += 1
            }
       for(var j=0;j<data.outwidth;j++)
            for(var i=0;i<data.height + 4;i++){
                data.layers[cnt].row = i
                data.layers[cnt].col = data.inwidth + j;
                cnt += 1
            }
        var color = d3.scale.category20();
        var g = svg.selectAll("rect").data(data.layers).enter()
            .append("g")
        g.append("rect").attr("x",function(d){return leftPadding + d.col * (width + padding)})
            .attr("y",function(d){return d.row*(height + padding)})
            .attr("width",width).attr("height",height)
            .attr("fill",function(d){return color(d.name)})
        if (width >= 100)
        g.append("text").attr("x",function(d){return (width/3) + d.col * (width + padding)})
            .attr("y",function(d){return d.row * (height+padding) + (height - 5) })
            .html(function(d){return d.name})
        
        var tooltip = d3.select("body").append("div")
            .attr("class", "tooltip")
            .style("opacity", 0);

        g.on("mouseover", function(d,i) {
            tooltip.transition()
                .duration(200)
                .style("opacity", .9);
            var text = createTooltip(d)
            tooltip.html(text)
                .style("left", (d3.event.pageX) + "px")
                .style("top", (d3.event.pageY - 50) + "px")
            tooltip.style("height",text.split("<br/>").length*15)
            d3.select(this).style("opacity", .7)
            })
        .on("mouseout", function(d) {
            tooltip.transition()
                .duration(500)
                .style("opacity", 0)
            d3.select(this).style("opacity", 1)
        });
    }
    
    function ModelGraph(divId, rawdata){
        var data = JSON.parse(rawdata)
        globaldata = data
        var div = d3.select("#"+divId).attr("style","height:500px;overflow:scroll;")
        if (data.model == "Net")
            sequential(div,data) 
        else if (data.model == "SeqToSeq")
            seqToSeq(div,data,10,10,2)
    }    
    exports.ModelGraph = ModelGraph
})(this)

