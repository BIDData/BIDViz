package BIDMach.ui

import BIDMat.{Mat,FMat,Image,MatFunctions}
import java.util.concurrent.Future;
import BIDMach.Learner
import BIDMach.networks.layers._
import play.api.libs.json._
    
import javax.swing._;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import java.awt.event._

class GraphFrame(val layers:Array[Map[String,JsValue]])extends JFrame("Graph"){
    val graph = new mxGraph();
    val _parent = graph.getDefaultParent();
    graph.getModel().beginUpdate();
    var i = 0
    val nodes = layers.map(d=>{
        val n = graph.insertVertex(_parent, null, d("toDisplay"), 20, 20 + 40*i, 200,30)
        i+=1
        n
    })
        
    graph.getModel().endUpdate();
    
    val graphComponent = new mxGraphComponent(graph) {
        override def installDoubleClickHandler(){
            getGraphControl()
                .addMouseListener(new MouseAdapter(){
                    override def mouseClicked(e: MouseEvent){
                        if(e.getClickCount()==2){
                            val cell = getCellAt(e.getX(), e.getY());
                            if (cell != null)
                                Plot.plot_layer_code(graph.getLabel(cell).split("\"")(1));
                        }
                    }
                })
        }
    }
    getContentPane().add(graphComponent);  
    graphComponent.getGraphControl()
        .addMouseListener(new MouseAdapter()
                          {
                              override def mouseReleased(e:MouseEvent)
                              {
                                  val  cell = graphComponent.getCellAt(e.getX(), e.getY());

                                  if (cell != null)
                                  {
                                      println("cell="+graph.getLabel(cell));
                                  }
                              }                              
                          });       
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
        server.send(utils.getModelGraph(learner.model)._2.toString)
        null
    }
    
    def plot_jframe(learner:Learner,fn:Layer=>String = _.getClass.getSimpleName) = {
        val obj = utils.getModelGraph(learner.model,fn)._2.asInstanceOf[JsValue]
        val layers = (obj \ "layers").as[Array[Map[String,JsValue]]]
        val frame = new GraphFrame(layers)
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame
    }    
    
    def plotGraph(learner:Learner,fn:Layer=>String = _.getClass.getSimpleName):GraphFrame = {
        if (useWeb) plot_web(learner)
        else plot_jframe(learner,fn)
    }
    
    def plot_code(filename:String) = {
        val title = filename.split("/").reverse.head
        val frame = new JFrame(title);
        val textArea = new JTextArea(60, 40);
        val scrollPane = new JScrollPane(textArea);
        val s=scala.io.Source.fromURL(filename).getLines.mkString("\n");
        textArea.append(s)        
        frame.getContentPane().add(scrollPane)
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame
    }
    
    def plot_layer_code(layerName: String) = {
        val bidmachURL = "https://raw.githubusercontent.com/BIDData/BIDMach/master/src/main/"
        plot_code(bidmachURL + "scala/BIDMach/networks/layers/" + layerName + ".scala")
    }
}
    
