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

function postData(data, callback, failure) {
    $.ajax({
        method: 'POST',
        url: '/request',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function (event) {
            console.log(event);
            if (callback) {
                callback(event);
            }
        },
        error: function (event, status, error) {
            console.log(event, status, error);
            console.log(failure);
            if (failure) {
                failure(event.responseText);
            }
        }
    });
}

function createGraphSuit(id) {
    var graph_suit = $("<div></div>").addClass("graph_suit").addClass("col-md-6").attr("id", id + '_suit').css("width", "50%").css("height", "450px").css("margin-bottom", "60px");
    var loadCode = $('<button>');
    loadCode.html("Modify");
    loadCode.addClass('chart');
    loadCode.attr("statname", id);
    graph_suit.append(loadCode);
    var graph = $("<div></div>").attr("id", id).css("margin", "0 auto");
    graph_suit.append(graph);
    var select_option = $("<option></option>").val(id).text(id).attr("selected", "selected");
    var graph_selector = $('#graph_selector');
    graph_selector.append(select_option);
    $('#graph_selector').multiselect("rebuild");
    return graph_suit;
}


function C3LineChart(id, name, size) {
    this.name = name;
    this.id = id;
    this.definition = {
        bindto: '#' + this.id,
        data: {
            x: 'x',
            columns:[ ['x'], ['y']]
        }
    };
    this.chart = c3.generate(this.definition);
}

C3LineChart.prototype.addPoint = function (ipass, sizes, values) {
    this.definition.data.columns[0].push(ipass);
    this.definition.data.columns[1].push(values[0]);
    this.chart.load(this.definition.data);
}

function VegaLiteChart(id, name, size) {
    this.id = id;
    this.name = name;
    this.size = size;
    this.definition = {
        "data": {
            "values": []
        },
        "mark": "point",
        "encoding": {
            "x": {
                "field": "0", "type": "quantitative", "range": "width",
                "scale": {"zero": false}
            },
            "y": {"field": "1", "type": "quantitative"}
        }
    };
    var embedSpec = {
        mode: "vega-lite",  // Instruct Vega-Embed to use the Vega-Lite compiler
        spec: this.definition
        // You can add more vega-embed configuration properties here.
        // See https://github.com/vega/vega/wiki/Embed-Vega-Web-Components#configuration-propeties for more information.
    };
    this.data = [];
    // Embed the visualization in the container with id `vis`
    vg.embed('#' + id, embedSpec, function (error, result) {
    });
}

VegaLiteChart.prototype.addPoint = function (ipass, sizes, values) {
    this.data.push([ipass, Number(values[0])]);
    this.definition.data.values = this.data.slice(this.data.length - 20);
    var embedSpec = {
        mode: "vega-lite",  // Instruct Vega-Embed to use the Vega-Lite compiler
        spec: this.definition
    };
    vg.embed('#' + this.id, embedSpec, function (error, result) {
        console.log(error, result);
    });
}


// Common interface for charts is addPoint( point ) where point is a matrix of conforming shape
function LineChart(id, name, size) {
    this.id = id;
    this.name = name;
    this.shape = [1, 1];
    this.size = size;
    if (!this.size) {
        this.size = 1;
    }
    var series = [];
    for (var i = 0; i < size; i++) {
        series.push({
            data: []
        });
    }
    this.definition = {
        chart: {
            renderTo: id,
            defaultSeriesType: 'spline',
            alighTicks: false,
            events: {}
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
            // type: 'linear',
            tickPixelInterval: 20,
            minRange: 40,
            tickInterval: 5
        },
        yAxis: {
            minPadding: 0.2,
            maxPadding: 0.2,
            title: {
                text: 'Value',
                margin: 30
            },
            min: 0,
            max: 1,
            endOnTick: false,
            startOnTick: false,
        },
        series: series
    };
    this.chart = new Highcharts.Chart(this.definition);
    console.log("here", series.length);
}

// point is (ipass, point)
LineChart.prototype.addPoint = function (ipass, sizes, values) {
    var series = this.chart.series;
    console.log("values", values);
    for (var i = 0; i < this.size; i++) {
        console.log(series);
        var shift = series[i].data.length > 40 / 2;
        var point = [ipass, +(values[i])];
        series[i].addPoint(point, true, shift);
    }
};

function Histogram(id, name) {
    this.id = id;
    this.name = name;
    this.definition = {
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
    };
    this.chart = new Highcharts.Chart(this.definition);
}

Histogram.prototype.addPoint = function (ipass, sizes, values) {
    var datas = [];
    for (var i = 0; i < sizes[0]; i += sizes[1]) {
        var p = [Number(values[i]), Number(values[i + 1])];
        datas.push(p);
    }
    console.log("calling setData", datas);
    this.chart.series[0].setData(datas, true, true, true);
}

function ScatterPlot(id, name) {
    this.id = id;
    this.name = name;
    this.definition = {
        chart: {
            renderTo: id,
            type: 'scatter',
            zoomType: 'xy',
        },
        title: {
            text: name
        },
        xAxis: {
            startOnTick: true,
            endOnTick: true,
            showLastLabel: true
        },
        series: [{
            name: name,
            color: 'rgba(119, 152, 191, .5)',
            data: [],
            pointPadding: 0,
            groupPadding: 0,
            pointPlacement: 'between'
        }]
    };
    this.chart = new Highcharts.Chart(this.definition);
}

ScatterPlot.prototype.addPoint = function (ipass, sizes, values) {
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
    this.paraMap = {};
    this.connect();

    this.ongoingRequest = {}
    this.requestCount = 0
}

// Send data to server
VizManager.prototype.sendData = function (data, callback) {
    var id = 'request' + this.requestCount;
    var message = {
        id: id,
        content: data
    };
    if (callback != null) {
        console.log("addedRequest");
        this.ongoingRequest[id] = callback;
        console.log(this.ongoingRequest);
    }
    this.requestCount++;
    console.log("sendData", message);
    this.websocket.send(JSON.stringify(message));
}

VizManager.prototype.handleCallback = function (content) {
    console.log("callback", content.id, this.ongoingRequest);
    if (content.id in this.ongoingRequest) {
        callback = this.ongoingRequest[content.id];
        if (callback != null) {
            callback(content);
        }
        delete this.ongoingRequest[content.id];
    } else {
        console.log("no request ");
    }
}

VizManager.prototype.connect = function () {
    this.endPoint = "ws://" + window.location.host + "/ws";
    if (this.websocket !== null) {
        this.websocket.close();
    }
    this.websocket = new WebSocket(endPoint);
    this.websocket.onmessage = this.onmessage.bind(this);
    this.websocket.onopen = this.onopen.bind(this);
    this.websocket.onclose = this.onclose.bind(this);
    this.websocket.onerror = this.onerror.bind(this);
    console.log("websocket connected");
    return true;
}


VizManager.prototype.handleDataPoint = function (object) {
    var name = object.name;
    if (!(name in this.allCharts)) {
        return;
        // var chart = this.createGraph(name, object.type, object.shape);
        this.allCharts[name] = chart;
    }
    var series = this.allCharts[name].addPoint(object.ipass, object.shape, object.data);
};

VizManager.prototype.handleParameters = function (object) {
    console.log("here");
    $("#parameters_body").html("");
    console.log("clean");
    for (var key in object) {
        var item = $('<tr>');
        var cell1 = $('<td>');
        cell1.html(key);
        var cell2 = $('<td>');
        cell2.attr('name', key);
        cell2.html(object[key]);
        item.append(cell1);
        item.append(cell2);
        $('#parameters_body').append(item);
    }
    console.log("filled");
    $('#parameters_table').editableTableWidget();

    var self = this;
    $('table td').on('change', function (evt, newValue) {
        var key = $(evt.currentTarget).attr('name');
        var name = newValue;
        self.paraMap[key] = name;
        return true;
    });
};

VizManager.prototype.onmessage = function (event) {
    console.log("raw data", event.data);
    if (event.data.length == 0) {
        console.log("empty message");
        return;
    }
    var msgs = $.parseJSON(event.data);
    for (var x in msgs) {
        var msg = msgs[x];
        var object = msg.content;
        switch (msg.msgType) {
            case 'data_point':
                this.handleDataPoint(object);
                break;
            case 'parameters':
                this.handleParameters(object);
                break;
            case 'error_message':
                $('#message').terminal().error(object.msg);
                break;
            case 'callback':
                console.log("callback");
                this.handleCallback(object);
                break;
        }
    }
}

VizManager.prototype.createGraph = function (name, type, shape) {
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
    } else if (type === 'VegaLiteChart') {
        chart = new VegaLiteChart(name, name);
    } else if (type == 'ScatterPlot') {
        chart = new ScatterPlot(name, name);
    } else if (type == 'C3LineChart') {
        chart = new C3LineChart(name, name);
    }
    this.allCharts[name] = chart;
    return chart;
}

VizManager.prototype.onopen = function (event) {
    console.log("onopen.");
    // this.websocket.send("");
    this.sendData({methodName: "requestParam"});
}
VizManager.prototype.onclose = function (event) {
}
VizManager.prototype.onerror = function (event) {
}
VizManager.prototype.addStat = function (obj, callback, failure) {
    var data = {
        methodName: "addFunction",
        content: obj
    };
    this.sendData(data, callback);
}

VizManager.prototype.pauseTraining = function (value, callback) {
    var data = {
        methodName: "pauseTraining",
        content: value
    };
    this.sendData(data, callback);
}

VizManager.prototype.modifyParam = function () {
    var data = {
        methodName: "modifyParam",
        content: []
    };
    console.log(this.paraMap);
    for (var key in this.paraMap) {
        var value = this.paraMap[key];
        data.content.push({'key': key, 'value': value});
    }
    console.log(data);
    this.sendData(data);
}

VizManager.prototype.evalCommand = function (code, callback) {
    var data = {
        methodName: "evaluateCommand",
        content: {
            "code": code
        }
    };
    this.sendData(data, callback);
}

VizManager.prototype.getCode = function (name, callback) {
    var data = {
        methodName: "getCode",
        content: {
            'name': name
        }
    };
    this.sendData(data, function(result) {
        console.log("code", result);
        callback({
            success: result.success,
            content: result.data
        });
    });
};

VizManager.prototype.createAllGraph = function() {
    var data = {
        methodName: "getCode"
    };
    this.sendData(data, function(result) {
        console.log(result);
    }.bind(this));
}

VizManager.prototype.saveMetrics = function() {
    var data = {
        methodName: "saveMetrics"
    };
    this.sendData(data, function(result) {
        console.log(result);
    }.bind(this));
}
