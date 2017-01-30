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
        },
        failure: function(event) {
            console.log(event);
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
    if (!this.size) {
        this.size = 1;
    }
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
            alighTicks: false,
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
    this.chart = new Highcharts.Chart({
        chart: {
            renderTo: id,
            type: 'column',
            alighTicks: false,
        },
        title: {
            text: name
        },
        xAxis: {
            gridLineWidth: 1
        },
        series: [{
            name: name,
            type: 'column',
            data: [],
            pointPadding: 0,
            groupPadding: 0,
            pointPlacement: 'between'
        }]
    });
}

Histogram.prototype.addPoint = function(ipass, sizes, values) {
    var datas = [];
    for (var i = 0; i < sizes[0]; i += sizes[1]) {
        var p = [Number(values[i]), Number(values[i + 1])];
        datas.push(p);
    }
    console.log("calling setData", datas);
    this.chart.series[0].setData(datas, true, true, true);
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
    console.log("raw data", event.data);
    if (event.data.length == 0) {
        console.log("empty message");
        return;
    }
    var msg = $.parseJSON(event.data);
    if (msg.msgType === 'data_point') {
        var object = msg.content;
        var name = object.name;
        if (!(name in this.allCharts)) {
            var chart = this.createGraph(name, object.type, object.shape);
            this.allCharts[name] = chart;
        }
        var series = this.allCharts[name].addPoint(object.ipass, object.shape, object.data);
    } else {
        $('#parameters').html("");
        for (var key in msg.content) {
            var item = $('<li>')
            item.html(key + ': ' + msg.content[key]);
            $('#parameters').append(item);
        }
    }
}

VizManager.prototype.createGraph = function(name, type, shape) {
    var chart;
    if ((name in this.allCharts)) {
        return;
    }
    var graphSuit = createGraphSuit(name);
    $('#' + this.root).append(graphSuit);
    if (type === 'LineChart') {
        var size = shape[0] * shape[1];
        chart = new LineChart(name, name, size);
    } else if (type === 'ColumnChart') {
        chart = new Histogram(name, name);
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

