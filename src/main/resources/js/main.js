(function(exports){
        
    function Viz(id){
        var s={}
        var dict = {}
        s.update =function(data){
            if (!(data.id in dict)){
                d3.select(id).append("div").attr("id",data.id)
                if (data.id == "graph")
                    dict[data.id] = ModelGraph(data.id,data)
                else {
                    dict[data.id] = new VegaLiteChart(data.id,null,{"config":{"data":data.names}})
                    utils.draggable(data.id)                
                }
            }
            if (data.id!="graph")
                dict[data.id].addPoint(data.ipass,null,data.values)
        }
        s.dict = dict
        return s
    }
    
    exports.Main = function(){
        var viz = Viz("#panel")
        var ws = new WebSocket("ws://" + window.location.host + "/ws")
        ws.onmessage = function (event) {
            console.log("Message: " + event.data)            
            viz.update(JSON.parse(event.data));
        };
        ws.onopen = function (event) {
            console.log("onopen.");
        };
        ws.onclose = function (event) {
            console.log("onclose.");
        };
        ws.onerror = function (event) {
            console.log("Error!");
        };
        return viz
    }
    
    exports.test = function() {
        var viz = Viz("#panel")
        var ipass = 0
        func = function(){
            ipass += 1
            viz.update({id:"t",names:["t","v"],ipass:ipass,values:[Math.random(),Math.random()]})
        }
        setTimeout(function(){viz.update({id:"graph",model:"Net",layers:[
            {name:"LinLayer",internalLayers:[]},
            {name:"RectLayer",internalLayers:[]}
        ]})},100)
        
        //setInterval(func,1000)
        return viz
    }
})(this)

//viz = Main()
viz = test()