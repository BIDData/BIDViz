package BIDMach.ui

import BIDMach.Learner
import BIDMat.{FMat, GMat, Mat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach.models._
import BIDMach.networks._
import BIDMach.networks.layers._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object utils {
    def getLayerInfo(layer:Layer,fn:Layer=>String = _.getClass.getSimpleName): JsObject =
      if (layer == null) JsObject(List()) else
      Json.obj("name" -> layer.getClass.getSimpleName,
               "imodel" -> (layer match {case ml:ModelLayer=>ml.imodel;case _ => -1}) ,
               "modelName" -> (layer match {case ml:ModelLayer=>ml.opts.modelName;case _ => ""}),
               "inputDim" -> layer._inputs.map(i=>
                   if (i == null) "" else {                                               
                       val m = i.layer._outputs(i.term);
                       if (m == null)"@"+layer.getClass.getSimpleName else m.nrows+"*"+m.ncols
                   }),
               "outputDim" -> layer._outputs.map(m=>if (m == null)"@"+layer.getClass.getSimpleName else m.nrows+"*"+m.ncols),
               "modelDim" -> (layer match {
                   case ml:ModelLayer=>{
                       val m = ml.modelmats(ml.imodel);
                       m.nrows+"*"+m.ncols
                   }
                   case _ => ""
               }),
               "internalLayers" -> (layer match {
                   case ml:CompoundLayer=>ml.internal_layers.map(getLayerInfo(_,fn));
                   case _ => Array[JsObject]()
               }),
               "toDisplay" -> fn(layer)
              )
        
//  def getModelGraph(learner: Learner):(Boolean, JsObject) = getModelGraph(learner.model)

  def getModelGraph(model: Model,fn:Layer=>String = _.getClass.getSimpleName):(Boolean, JsObject) = 
        model match {
          case m:SeqToSeq =>              
              val layersInfo = m.layers.map(getLayerInfo(_,fn))    
              (true,Json.obj("model" -> m.getClass.getSimpleName,
                             "inwidth" -> m.opts.inwidth,
                             "outwidth" -> m.opts.outwidth,                             
                             "height" -> m.opts.height,
                             "layers" -> layersInfo,
                             "id" -> "graph"))
          case m:Net =>
              val layersInfo = m.layers.map(getLayerInfo(_,fn))    
              (true,Json.obj("model" -> m.getClass.getSimpleName,
                             "layers" -> layersInfo,
                             "id" -> "graph"))
          case _ =>
              (true,null)//Json.obj("[\"layer1\",\"layer2\"]"))
      }
    
  val bidmachDir = "/raid/byeah/BIDMach/"
  val bidmachURL = "https://raw.githubusercontent.com/BIDData/BIDMach/master/src/main/"
  def process(learner: Learner)(msg:String) = {
      val data = msg.split("/")          
      if (data.head == "getCode")
//          scala.io.Source.fromFile(bidmachDir + "src/main/scala/BIDMach/networks/layers/"+data(1)).getLines.mkString("\n")
          scala.io.Source.fromURL(bidmachURL + "scala/BIDMach/networks/layers/" + data(1)).getLines.mkString("\n")
  }
}