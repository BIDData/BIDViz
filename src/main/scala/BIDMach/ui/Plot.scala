package BIDMach.ui

import BIDMat.{Mat,FMat,Image,MatFunctions}
import java.util.concurrent.Future;
import BIDMach.Learner


object Plot{
    var server: WebServer = null
    def start(port:Int = 8890, learner:Learner = null) {
        server = new WebServer(port,utils.process(learner) _)
    }
    var tot = 0
    var interval = 1
        
    def getMsg(id:String, ipass:Int, names:List[String], values:Array[Float]) = {
         "{\"id\":\"%s\",\"ipass\":%d,\"names\":[%s],\"values\":[%s]}" format(id,ipass,names.map('"'+_+'"').reduce(_+","+_),values.map('"'+_.toString+'"').reduce(_+","+_))                    
    }
        
    def plot(fn:()=>Mat,names:List[String]=List("data")) {
        if (server == null)
            start()
        tot += 1
        val id = "Plot_" + tot
        var ipass = 0
        val runme = new Runnable {
          override def run() = {
            while (true) {
                Thread.sleep((1000*interval).toLong);
                val mat = MatFunctions.cpu(fn());
                ipass += 1
                val message = getMsg(id,ipass,names,FMat(mat).data)
                server.send(message)
//                println(message)
            }
          }
        }
        Image.getService.submit(runme);
    }
    
    def plot(learner:Learner) {
        if (server == null)
            start()
        server.send(utils.getModelGraph(learner)._2)
    }
}