/**
 * Created by zouxuan on 11/19/16.
 */
var chart;
var current = 0;
var idIndex = 0;

function requestData() {
    $.ajax({
        dataType: 'json',
        url: 'server.php?step=' + current,
        success: function (data) {
            var series = chart.series[0],
                shift = series.data.length > 40; // shift if the series is
                                                 // longer than 20
            // var object=$.parseJSON(data);
            // alert(object.name);
            // add the point
            var point = [current, data[2]];
            current += 1;
            chart.series[0].addPoint(point, true, shift);

            // call it again after one second
            setTimeout(requestData, 1000);
        },
        cache: false
    });
}

function update(data) {
    var object = $.parseJSON(data);
    var point = [Number(object.ipass), Number(object.value)];
    console.log(point)
    var series = chart.series[0];
    var shift = series.data.length > 40;
    chart.series[0].addPoint(point, true, shift);
}

$(document).ready(function () {
    $('#start').click(function () {
        console.log("i am here");
        connectToWS();
    });
    createGraphSuit("container" + idIndex);
    createHighcharts("container" + idIndex);
    idIndex++;
    $("#creater").click(function () {

    });
    $(".creator").click(function (event) {
        name=event.
        createGraphSuit("container" + idIndex);
        createHighcharts("container" + idIndex);
        idIndex++;
    })

    // chart = new Highcharts.Chart({
    //     chart: {
    //         renderTo: 'container',
    //         defaultSeriesType: 'spline',
    //         events: {
    //             load: requestData
    //         }
    //     },
    //     title: {
    //         text: 'Training Data',
    //         style: {
    //             color: '#000',
    //             fontWeight: 'bold',
    //             fontSize: "20px",
    //         }
    //     },
    //     xAxis: {
    //         type: 'linear',
    //         tickPixelInterval: 40,
    //         maxZoom: 40
    //     },
    //     yAxis: {
    //         minPadding: 0.2,
    //         maxPadding: 0.2,
    //         title: {
    //             text: 'Value',
    //             margin: 80
    //         }
    //     },
    //     series: [{
    //         name: 'Random data',
    //         data: []
    //     }]
    // });
});

function createGraphSuit(id) {
    var graph_suit = $("<div></div>").addClass("graph_suit col-md-6").css("margin-bottom", "60px");
    var graph = $("<div></div>").attr("id", id).css("height", "450px").css("margin", "0 auto");
    var button_wrap = $("<div></div>").addClass("col-md-2 col-md-offset-5 button_suit");
    var button = $("<a></a>").addClass("btn btn-primary btn-sm").attr("id", "start").text("Get Data");
    $("#graph").append(graph_suit);
    graph_suit.append(graph);
    graph_suit.append(button_wrap);
    button_wrap.append(button);
}

function createHighcharts(id) {
    chart = new Highcharts.Chart({
        chart: {
            renderTo: id,
            defaultSeriesType: 'spline',
            events: {
                load: requestData
            }
        },
        title: {
            text: 'Training Data',
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
            name: 'Random data',
            data: []
        }]
    });
}

