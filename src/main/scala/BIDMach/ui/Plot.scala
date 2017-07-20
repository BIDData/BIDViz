package BIDMach.ui

import BIDMat.{Mat,FMat,Image,MatFunctions}
import java.util.concurrent.Future;
import BIDMach.Learner
import play.api.libs.json._
import javax.swing.JFrame;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

class GraphFrame(val layers:Array[Map[String,JsValue]])extends JFrame("Graph"){
    val graph = new mxGraph();
    val _parent = graph.getDefaultParent();
    graph.getModel().beginUpdate();
    var i = 0
    val nodes = layers.map(d=>{
    graph.insertVertex(_parent, null, d("name"), 20, 20 + 40*i, 200,30)
        i+=1
    })
        //            graph.insertEdge(parent2, null, "Edge", v1, v2);
    graph.getModel().endUpdate();

    val graphComponent = new mxGraphComponent(graph);
    getContentPane().add(graphComponent);    
}

object Plot{
    var server: WebServer = null
    def start(port:Int = 8890, learner:Learner = null) {
        server = new WebServer(port,utils.process(learner) _)
    }
    var tot = 0
    var interval = 1
    var useWeb = false // true if using web based visualization, otherwise JFrame based visualization will be called
        
        
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
    
    def plot_web(learner:Learner) = {        
        if (server == null)
            start()
        server.send(utils.getModelGraph(learner)._2.toString)
        null
    }
    
    def plot_jframe(learner:Learner) = {
        val obj = utils.getModelGraph(learner)._2.asInstanceOf[JsValue]
        val layers = (obj \ "layers").as[Array[Map[String,JsValue]]]
        val frame = new GraphFrame(layers)
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame
    }
    
    def plot(learner:Learner):GraphFrame = {
        if (useWeb) plot_web(learner)
        else plot_jframe(learner)
    }
}
    
