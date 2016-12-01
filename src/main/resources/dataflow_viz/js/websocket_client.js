/**
 * Created by zouxuan on 11/30/16.
 */


var myWebSocket;
var endPoint = "";

function connectToWS() {
    if (myWebSocket !== undefined) {
        myWebSocket.close()
    }
    myWebSocket = new WebSocket(endPoint);
    myWebSocket.onmessage = function (event) {
        // window.alert("Message: " + event.data)
        update(data);
    };
    myWebSocket.onopen = function (event) {
        console.log("onopen.");
    };
    myWebSocket.onclose = function (event) {
        console.log("onclose.");
    };
    myWebSocket.onerror = function (event) {
        console.log("Error!");
    };

    function sendMsg() {
        var message = document.getElementById("myMessage").value;
        myWebSocket.send(message);
    }

    function closeConn() {
        myWebSocket.close();
    }


}