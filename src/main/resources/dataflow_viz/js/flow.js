/**
 * Created by zouxuan on 11/19/16.
 */
var chart;
var current=0;
function requestData() {
    $.ajax({
        dataType:'json',
        url: 'server.php?step='+current,
        success: function(data) {
            var series = chart.series[0],
                shift = series.data.length > 40; // shift if the series is
                                                 // longer than 20
            // var object=$.parseJSON(data);
            // alert(object.name);
            // add the point
            var point=[current,data[2]];
            current+=1;
            chart.series[0].addPoint(point, true, shift);
        
            // call it again after one second
            setTimeout(requestData, 1000);
        },
        cache: false
    });
}

function update(data){
    var object=$.parseJSON(data);
    var point=[data.x,data.y];
    var series=chart.series[0];
    var shift=series.data.length>40;
    chart.series[0].addPoint(point,true,shift);
}

$(document).ready(function() {
    chart = new Highcharts.Chart({
        chart: {
            renderTo: 'container',
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
});