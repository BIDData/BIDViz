(function (H) {
    'use strict';
    var addEvent = H.addEvent,
        each = H.each,
        doc = document,
        body = doc.body;

    H.wrap(H.Chart.prototype, 'init', function (proceed) {

        // Run the original proceed method
        proceed.apply(this, Array.prototype.slice.call(arguments, 1));

        var chart = this,
            renderer = chart.renderer,
            yAxis = chart.yAxis;

        each(yAxis, function (yAxis) {
            var options = yAxis.options,
                scalable = options.scalable === undefined ? true : options.scalable,
                labels = options.labels,
                pointer = chart.pointer,
                labelGroupBBox,
                bBoxX,
                bBoxY,
                bBoxWidth,
                bBoxHeight,
                isDragging = false,
                downYValue;

            if (scalable) {
                bBoxWidth = 40;
                bBoxHeight = chart.containerHeight - yAxis.top - yAxis.bottom;
                bBoxX = yAxis.opposite ? (labels.align === 'left' ? chart.containerWidth - yAxis.right : chart.containerWidth - (yAxis.right + bBoxWidth)) : (labels.align === 'left' ? yAxis.left : yAxis.left - bBoxWidth);
                bBoxY = yAxis.top;

                // Render an invisible bounding box around the y-axis label group
                // This is where we add mousedown event to start dragging
                labelGroupBBox = renderer.rect(bBoxX, bBoxY, bBoxWidth, bBoxHeight)
                    .attr({
                        fill: '#fff',
                        opacity: 0,
                        zIndex: 8
                    })
                    .css({
                        cursor: 'ns-resize'
                    })
                    .add();

                labels.style.cursor = 'ns-resize';

                addEvent(labelGroupBBox.element, 'mousedown', function (e) {
                    var downYPixels = pointer.normalize(e).chartY;

                    downYValue = yAxis.toValue(downYPixels);
                    isDragging = true;
                });

                addEvent(chart.container, 'mousemove', function (e) {
                    if (isDragging) {
                        body.style.cursor = 'ns-resize';

                        var dragYPixels = chart.pointer.normalize(e).chartY,
                            dragYValue = yAxis.toValue(dragYPixels),

                            extremes = yAxis.getExtremes(),
                            userMin = extremes.userMin,
                            userMax = extremes.userMax,
                            dataMin = extremes.dataMin,
                            dataMax = extremes.dataMax,

                            min = userMin !== undefined ? userMin : dataMin,
                            max = userMax !== undefined ? userMax : dataMax,

                            newMin,
                            newMax;

                        // update max extreme only if dragged from upper portion
                        // update min extreme only if dragged from lower portion
                        if (downYValue > (dataMin + dataMax) / 2) {
                            newMin = min;
                            newMax = max - (dragYValue - downYValue);
                            newMax = newMax > dataMax ? newMax : dataMax; //limit
                        } else {
                            newMin = min - (dragYValue - downYValue);
                            newMin = newMin < dataMin ? newMin : dataMin; //limit
                            newMax = max;
                        }

                        yAxis.setExtremes(newMin, newMax, true, false);
                    }
                });

                addEvent(document, 'mouseup', function () {
                    body.style.cursor = 'default';
                    isDragging = false;
                });

                // double-click to go back to default range
                addEvent(labelGroupBBox.element, 'dblclick', function () {
                    var extremes = yAxis.getExtremes(),
                        dataMin = extremes.dataMin,
                        dataMax = extremes.dataMax;

                    yAxis.setExtremes(dataMin, dataMax, true, false);
                });
            }
        });
    });
}(Highcharts));

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
    var graph_suit = $("<div></div>").addClass("graph_suit").css("margin-bottom", "60px");
    var graph = $("<div></div>").attr("id", id).addClass('row').css("height", "450px").css("margin", "0 auto");
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
    var series = [];
    for (var i = 0; i < size; i++) {
        console.log("i am here")
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
            },
            min: 0,
            max: 1,
            endOnTick: false,
            startOnTick: false,
        },
        series: series
    });
    console.log("here", series.length);
}

// point is (ipass, point)
LineChart.prototype.addPoint = function(ipass, sizes, values) {
    var series = this.chart.series;
    console.log("values", values);
    for (var i = 0; i < this.size; i++) {
        console.log(series);
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

