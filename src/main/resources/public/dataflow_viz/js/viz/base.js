(function(exports){    
    var dictOfVizClass = {}
    
    function BaseViz(name,vizClass){
        this.name = name
        dictOfVizClass[name] = vizClass
    }

    BaseViz.prototype.addPoint = function (ipass, sizes, values) {
        var msg = "addPoint not implemented for " + this.name
        console.log(msg)
        alert(msg)
    }
    
    exports.BaseViz = BaseViz
    exports.dictOfVizClass = dictOfVizClass
    
})(this)
