function createGraphSuit(id) {
    var graph_suit = $("<div></div>").addClass("graph_suit col-md-6").css("margin-bottom", "60px");
    var graph = $("<div></div>").attr("id", id).css("height", "450px").css("margin", "0 auto");
    graph_suit.append(graph);
    return graph_suit;
}

function createHighcharts(id, name) {
    return new Highcharts.Chart({
        chart: {
            renderTo: id,
            defaultSeriesType: 'spline',
            events: {
 //               load: requestData
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
            name: name,
            data: []
        }]
    });
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

VizManager.prototype.onmessage = function(event) {
    var object = $.parseJSON(event.data);
    var name = object.name;
    var point = [Number(object.ipass), Number(object.value)];
    console.log(event.data);
    if (!(name in this.allCharts)) {
        // create new
        var graphSuit = createGraphSuit(name);
        $('#' + this.root).append(graphSuit);
        var chart = createHighcharts(name, name);
        this.allCharts[name] = chart;
    }
    var series = this.allCharts[name].series[0];
    var shift = series.data.length > 40;
    series.addPoint(point, true, shift);
}

VizManager.prototype.onopen = function(event) {
    console.log("onopen.");
    this.websocket.send("hello");
}
VizManager.prototype.onclose = function(event) {}
VizManager.prototype.onerror = function(event) {}
VizManager.prototype.addStat = function(name, code) {
    var data = {
        name: name,
        code: code
    };
    $.ajax({
        method: 'POST',
        url: '/request',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(event) {
            console.log(event);
        }
    });
}