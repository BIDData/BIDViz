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
    console.log(typeof values[0])
    this.definition.data.columns[0].push(ipass);
    this.definition.data.columns[1].push(values[0]);
    this.chart.load(this.definition.data);
}
