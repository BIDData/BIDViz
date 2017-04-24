(function(exports){
    
    function Conv(id,name,size){
//        console.log("Here pixel")
        this.id = id
        this.name = name
        var div = d3.select("#"+id)
        div.append("div").append("tspan").html(name)
        var canvas = div.append("canvas").attr("height",600).attr("width",600)
        this.ctx = canvas.node().getContext("2d")
        var image = this.ctx.getImageData(0,0,300,300)
        for(var i=0;i<image.data.length;i+=4){
            image.data[i] = 0
            image.data[i+3] = 255
        }
//        console.log("pixel",image.data.slice(0,20))
        this.ctx.putImageData(image,0,0)
        var img = this.ctx.getImageData(0,0,300,300)
//        console.log("pixel",img.data.slice(0,20))        
    }
    
    Conv.prototype = new BaseViz("Conv",Conv)
    
    Conv.prototype.addPoint = function (ipass, sizes, values) {
        var n = sizes[0], m = sizes[1]
        var patch = Math.round(Math.sqrt(sizes[0] / 3))
        for(var i=0;i<values.length;i++)
            values[i] = parseInt(values[i]*1000) + 128
//        console.log()
        var n = Math.ceil(Math.sqrt(sizes[1]))
        var size = 2
        var image = this.ctx.getImageData(0,0,n*patch*size,n*patch*size)
        for(var i=0;i<n;i++)
            for(var j=0;j<n;j++)
            {
                if (i*n+j>=sizes[1])break;
                var k = i*n+j
                for(var r = 0;r<patch;r++)
                    for(var c = 0;c<patch;c++){       
                        var t = k*sizes[0]+r*patch+c;
                        for(var di=0;di<size;di++)
                            for(var dj=0;dj<size;dj++)
                            {
                                var off=(((i*patch+r)*size+di)*n*patch*size+(j*patch+c)*size+dj)*4;
                                image.data[off] = values[t]
                                image.data[off+1] = values[t + patch*patch]
                                image.data[off+2] = values[t + 2 * + patch*patch]
                                image.data[off+3] = 255
                            }
                    }
            }
        this.ctx.putImageData(image,0,0)
    }
    
    function OutputMap(id,name,size){
        this.id = id
        this.name = name
        var div = d3.select("#"+id)
        div.append("div").append("tspan").html(name)
        var canvas = div.append("canvas").attr("height",600).attr("width",600)
        this.ctx = canvas.node().getContext("2d")
        var image = this.ctx.getImageData(0,0,300,300)
        for(var i=0;i<image.data.length;i+=4){
            image.data[i] = 0
            image.data[i+3] = 255
        }
        this.ctx.putImageData(image,0,0)
        var img = this.ctx.getImageData(0,0,300,300)
    }
    
    OutputMap.prototype = new BaseViz("OutputMap",OutputMap)
    
    OutputMap.prototype.addPoint = function (ipass, sizes, values) {
        var n = sizes[0], m = sizes[1]
        var patch = Math.round(Math.sqrt(sizes[0] / 3))
        console.log("values",values.slice(0,20))
        for(var i=0;i<values.length;i++)
            values[i] = parseInt(values[i]) + 0
        var n = Math.ceil(Math.sqrt(sizes[1]))
        var size = 1
        var image = this.ctx.getImageData(0,0,n*patch*size,n*patch*size)
        for(var i=0;i<n;i++)
            for(var j=0;j<n;j++)
            {
                if (i*n+j>=sizes[1])break;
                var k = i*n+j
                for(var r = 0;r<patch;r++)
                    for(var c = 0;c<patch;c++){       
                        var t = k*sizes[0]+r*patch+c;
                        for(var di=0;di<size;di++)
                            for(var dj=0;dj<size;dj++)
                            {
                                var off=(((i*patch+r)*size+di)*n*patch*size+(j*patch+c)*size+dj)*4;
                                image.data[off] = values[t]
                                image.data[off+1] = values[t + patch*patch]
                                image.data[off+2] = values[t + 2 * + patch*patch]
                                image.data[off+3] = 255
                            }
                    }
            }
        console.log("data",image.data.slice(0,20))
        this.ctx.clearRect(0,0,n*patch*size,n*patch*size)
        this.ctx.putImageData(image,0,0)
    }
    
})(this)

