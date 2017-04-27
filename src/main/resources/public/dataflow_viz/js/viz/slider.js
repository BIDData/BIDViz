(function(exports){
    var valueList = [0]
    var v=0.00001
    for(var i=0;i<5;i++){
        for(var j=1;j<=9;j++)
            valueList.push(v*j)
        v*=10;
    }            
    valueList.push(0.95)    
    valueList.push(0.99)    
    valueList.push(1)    
    
    function Slider(div,name,parameterList){
        parameterList = parameterList || ["---","momentum","langevin","lrate"]
        var s={}
        var menu = div.append("div")
        var controls = div.append("controls")
        var currentSliders = {}
        
        menu.append("form").attr("action","#")
            .append("label").attr("for",name+"_menu").html("Select a parameter")
            .append("select").attr("name",name+"_menu").attr("id",name+"_menu")
            .on("change",function(){               
                var para = $("#"+name+"_menu").val()
                if (!(para in currentSliders) && (para!= "---")){
                    currentSliders[para] = true
                    var d = controls.append("div")
                    d.append("span").html(para).attr("style","width:100px;display: inline-block;")
                    d.append("input").attr("type","range")
                        .attr("style","width:200px;display: inline-block;").attr("max",valueList.length-1)
                        .attr("value",valueList.length-1)
                    var num = d.append("span").html("1.00000").attr("style","display: inline-block;")   
                    d.select("input").on("change",function(){
                        var v = valueList[parseInt($(d.select("input").node()).val())]
                        var data = {
                            methodName: "modifyParam",
                            content: [{"key":para,"value": v.toString() }]
                        };
                        viz.sendData(data)
                    })
                    .on("mousemove",function(){
                        var v = valueList[parseInt($(d.select("input").node()).val())]
                        num.html(v.toFixed(5))
                    })
                          
                }
            })
            .selectAll("option").data(parameterList).enter()
            .append("option").html(function(d){return d})
            
        /*$( "#"+name+"_menu" ).selectmenu({
          change: function( event, data ) {
//              controls.append()
              console.log(data.item.value)
          }
         });*/
        
        return s
    }
    
    exports.Slider = Slider
})(this)