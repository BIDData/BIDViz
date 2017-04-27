(function(exports) {
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
        this.modelGraph = null
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
        //console.log("raw data", event.data);
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
        var tmp = name.split("|")
        var id = tmp[0]
        var names = name
        if (tmp.length>1)
            names = tmp[1].split(",")
        if ((name in this.allCharts)) {
            alert("A visualization called " + name +" already existed")
            return;
        }
        var graphSuit = createGraphSuit(id);
        $('#' + this.root).append(graphSuit);
        /*if (type === 'LineChart') {
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
        }*/
        var size = shape[0] * shape[1];
        chart = new dictOfVizClass[type](id,names,size)
        this.allCharts[name] = chart;
        return chart;
    }

    VizManager.prototype.onopen = function (event) {
        console.log("onopen.");
        // this.websocket.send("");
        this.sendData({methodName: "requestParam"});
//        this.createModelGraph("modelgraph")
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

    VizManager.prototype.saveMetrics = function(name) {
        var data = {
            methodName: "saveMetrics",
            content: name
        };
        this.sendData(data, function(result) {
            console.log(result);
        }.bind(this));
    }
    VizManager.prototype.loadMetrics = function(name) {
        var data = {
            methodName: "loadMetrics",
            content: name
        };
        this.sendData(data, function(result) {
            console.log("loadMetrics");
            console.log(result.data);
            var data = JSON.parse(result.data).stats;
            for (var name in data) {
                var obj = data[name];
                var chart = this.createGraph(obj.name, obj.type, [obj.size, 1]);
                this.allCharts[name] = chart;
            }
        }.bind(this));
    }
    
    VizManager.prototype.createModelGraph = function(divId) {
        if (this.modelGraph != null) return
        var data = {
            methodName: "getModelGraph"
        }
        this.sendData(data,(function(result){
            if (result.success)
                this.modelGraph = new ModelGraph(divId,result.data)
        }).bind(this))
    }
    
    exports.VizManager = VizManager
})(this)
