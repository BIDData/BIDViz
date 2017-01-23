function postData(data, callback) {
    $.ajax({
        method: 'POST',
        url: '/request',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(event) {
            console.log(event);
            if (callback) {
                callback(event);
            }
        }
    });
}

function createGraphSuit(id) {
    var graph_suit = $("<div></div>").addClass("graph_suit col-md-6").css("margin-bottom", "60px");
    var graph = $("<div></div>").attr("id", id).css("height", "450px").css("margin", "0 auto");
    graph_suit.append(graph);
    return graph_suit;
}

// Common interface for charts is addPoint( point ) where point is a matrix of conforming shape
function LineChart(id, name, size) {
    this.id = id;
    this.name = name;
    this.shape = [1,1];
    this.size = size;
    series = [];
    for (var i = 0; i < size; i++) {
        series.push({
            data: []
        });
    }
    this.chart = new Highcharts.Chart({
        chart: {
            renderTo: id,
            defaultSeriesType: 'spline',
            events: {
            }
        },
        title: {
            text: name,
            style: {
                color: '#000',
                fontWeight: 'bold',
                fontSize: "20px",
            }
        },
        xAxis: {
            type: 'linear',
            tickPixelInterval: 40,
            maxZoom: 40
        },
        yAxis: {
            minPadding: 0.2,
            maxPadding: 0.2,
            title: {
                text: 'Value',
                margin: 80
            }
        },
        series: series
    });
}

// point is (ipass, point)
LineChart.prototype.addPoint = function(ipass, sizes, values) {
    var series = this.chart.series;
    console.log("values", values);
    for (var i = 0; i < this.size; i++) {
        var shift = series[i].data.length > 40;
        var point = [ipass, +(values[i])];
        series[i].addPoint(point, true, shift);
    }
}

function Histogram(id, name) {
    this.id = id;
    this.name = name;
    this.shape = [1,1];
    this.pointSet = [];
    this.step = 10;
    this.chart = new Highcharts.Chart({
        chart: {
            type: "column",
            renderTo: id,
            defaultSeriesType: 'spline',
            events: {
            }
        },
        title: {
            text: name,
            style: {
                color: '#000',
                fontWeight: 'bold',
                fontSize: "20px",
            }
        },
        xAxis: {
            type: 'linear',
            tickPixelInterval: 40,
            maxZoom: 40
        },
        yAxis: {
            minPadding: 0.2,
            maxPadding: 0.2,
            title: {
                text: 'Value',
                margin: 80
            }
        },
        series: [{
            data: []
        }]
    });
}

    function arrToHistogram(data, binsize) {
        var histo = {},
            x,
            i,
            arr = [];

        var max = Math.max(data);
        var min = Math.min(data);
        var step = (max - min) / binsize;
        if (step < 0.01) {
            return;
        }

        // Group down
        for (i = 0; i < data.length; i++) {
            x = Math.floor(data[i] / step) * step;
            if (!histo[x]) {
                histo[x] = 0;
            }
            histo[x]++;
        }

        // Make the histo group into an array
        for (x in histo) {
            if (histo.hasOwnProperty((x))) {
                arr.push([parseFloat(x), histo[x]]);
            }
        }

        // Finally, sort the array
        arr.sort(function (a, b) {
            return a[0] - b[0];
        });

        return arr;
    }

Histogram.prototype.addPoint = function(point) {
    var series = this.chart.series[0];
    var shift = series.data.length > 40;
    this.pointSet.push(point[1]);
    this.binsize = 10;
    var newBins = arrToHistogram(this.pointSet, this.binsize);
    // console.log('newbin', newBins);
    series.setData(newBins);
}

function VizManager(root) {
    // make VizManager
    // need to: control changes to the graph
    // can listen to websocket,
    // can send ajax to server
    this.websocket = null;
    this.allCharts = {};
    this.root = root;
    this.connect();
}

VizManager.prototype.connect = function() {
    this.endPoint = "ws://" + window.location.host + "/ws";
    if (this.websocket !== null) {
        this.websocket.close()
    }
    this.websocket = new WebSocket(endPoint);
    this.websocket.onmessage = this.onmessage.bind(this);
    this.websocket.onopen = this.onopen.bind(this);
    this.websocket.onclose = this.onclose.bind(this);
    this.websocket.onerror = this.onerror.bind(this);
    console.log("websocket connected");
    return true;
}


//        Json.obj(
//          "name" -> name,
//        "ipass" -> ipass,
//        "shape" -> shape,
//        "data" -> data
//      )

VizManager.prototype.onmessage = function(event) {
    var msg = $.parseJSON(event.data);
    if (msg.msgType === 'data_point') {
        var object = msg.content;
        var name = object.name;
        if (!(name in this.allCharts)) {
            // create new
            var graphSuit = createGraphSuit(name);
            $('#' + this.root).append(graphSuit);
            // var chart = new LineChart(name, name);
             var chart = new LineChart(name, name, 1);
            this.allCharts[name] = chart;
        }
        var series = this.allCharts[name].addPoint(object.ipass, object.sizes, object.data);
    } else {
        console.log(msg);
        $('#parameters').html("");
        for (var key in msg.content) {
            var item = $('<li>')
            item.html(key + ': ' + msg.content[key]);
            $('#parameters').append(item);
        }
    }
}

VizManager.prototype.createGraph = function(name, type, size) {
    var chart;
    if ((name in this.allCharts)) {
        return;
    }
    var graphSuit = createGraphSuit(name);
    $('#' + this.root).append(graphSuit);
    if (type === 'LineChart') {
        chart = new LineChart(name, name, size);
    } else if (type === 'Histogram') {
        chart = new Histogram(name, name, size);
    }
    this.allCharts[name] = chart;
    return chart;
}

VizManager.prototype.onopen = function(event) {
    console.log("onopen.");
    this.websocket.send("hello");
}
VizManager.prototype.onclose = function(event) {}
VizManager.prototype.onerror = function(event) {}
VizManager.prototype.addStat = function(obj, callback) {
    var data = {
        methodName: "addFunction",
        content: obj
    };
    postData(data, callback);
}

VizManager.prototype.pauseTraining = function(value, callback) {
    var data = {
        methodName: "pauseTraining",
        content: value
    };
    postData(data, callback);
}

VizManager.prototype.modifyParam = function(name, value) {
    var data = {
        methodName: "modifyParam",
        content: {
            name: name,
            value: value
        }
    };
    postData(data);
}

