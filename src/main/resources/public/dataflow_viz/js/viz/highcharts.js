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



