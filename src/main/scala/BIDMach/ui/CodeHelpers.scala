package BIDMach.ui

import BIDMach.Learner
import BIDMat.{FMat, GMat, Mat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach.networks._
import BIDMach.networks.layers._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by han on 1/29/17.
  *  Helpers to user input code that used to create
  *  charts
  */

object CodeHelpers {
  var lasti = 0
      
  def validScore(learner: Learner):Mat = {
    val len = learner.reslist.length
    val istart = if (learner.opts.cumScore == 0) learner.lasti else
      {if (learner.opts.cumScore == 1) 0 else if (learner.opts.cumScore == 2) len/2 else 3*len/4};
    var i = 0
    var sum = 0.0;
    for (scoremat <- learner.reslist) {
      if (i >= istart) sum += mean(scoremat(?,0)).v
      i += 1
    }
    var mat = ones(1,1)
    mat(0, 0) = (sum/(len - istart))
    mat
  }
    
  def trainScore(learner: Learner):Mat = {
    val len = learner.reslist.length
    val istart = if (learner.opts.cumScore == 0) learner.lasti else
      {if (learner.opts.cumScore == 1) 0 else if (learner.opts.cumScore == 2) len/2 else 3*len/4};
    var i = 0
    var sum = 0.0;
    for (scoremat <- learner.trainReslist) {
      if (i >= istart) sum += mean(scoremat(?,0)).v
      i += 1
    }
    var mat = ones(1,1)
    mat(0, 0) = (sum/(len - istart))
    mat
  }
    
  def loss(learner: Learner):Mat = {
    val len = learner.reslist.length
    val istart = if (learner.opts.cumScore == 0) lasti else
      {if (learner.opts.cumScore == 1) 0 else if (learner.opts.cumScore == 2) len/2 else 3*len/4};
    lasti = len
    var i = 0
    var mat = ones(2,1)
    var sum = 0.0;
    for (scoremat <- learner.reslist) {
      if (i >= istart) sum += mean(scoremat(?,0)).v
      i += 1
    }
    mat(0, 0) = (sum/(len - istart))
    sum = 0.0;
    i = 0
    for (scoremat <- learner.trainReslist) {
      if (i >= istart) sum += mean(scoremat(?,0)).v
      i += 1
    }
    mat(1, 0) = (sum/(len - istart))
    mat
  }
    
  

  def toHistogram(input: Mat, buckets:Int): Mat = {
    var result = zeros(buckets, 2)
    var maximun:Float = maxi(input(?), 1) match {
      case i: GMat => i (0, 0)
      case i: Mat => i.asInstanceOf[FMat] (0, 0)
    }
    var minimun:Float = mini(input(?), 1) match {
      case i: GMat => i (0, 0)
      case i: Mat => i.asInstanceOf[FMat] (0, 0)
    }
    if (maximun > minimun) {
      var size = (maximun - minimun) / buckets
      var f:FMat = FMat(input(?))
      for (elm <- 0 until f.nrows) {
        var current = f(elm)
        var position = ((current - minimun) / size).toInt
        if (position >= result.nrows) {
          position = result.nrows - 1
        }
        result(position, 0) = position * size + minimun
        result(position, 1) += 1
      }
    }
    return result
  }
    
  def getLayerInfo(layer:Layer): JsObject =
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
                   case ml:CompoundLayer=>ml.internal_layers.map(getLayerInfo(_));
                   case _ => Array[JsObject]()
               }))

  def getModelGraph(learner: Learner) = 
      learner.model match {
          case m:SeqToSeq =>              
              val layersInfo = m.layers.map(getLayerInfo(_))    
              (true,Json.obj("model" -> m.getClass.getSimpleName,
                             "inwidth" -> m.opts.inwidth,
                             "outwidth" -> m.opts.outwidth,                             
                             "height" -> m.opts.height,
                             "layers" -> layersInfo).toString)
          case m:Net =>
              val layersInfo = m.layers.map(getLayerInfo(_))    
              (true,Json.obj("model" -> m.getClass.getSimpleName,
                             "layers" -> layersInfo).toString)
          case _ =>
              (true,"[\"layer1\",\"layer2\"]")
  }
  
  def start(learner:Learner) = Future(learner.train)
      
  def setInterval(learner:Learner, time: Int) =
      learner.opts.observer match {
          case c: WebServerChannel => c.AVG_MESSAGE_TIME_MILLIS = time
          case _ =>
      }   
}
