package BIDMach.ui

import BIDMach.ui.Message.{contentWrite, messageWrite}
import BIDMach.ui.Message
import BIDMach.Learner
import BIDMat.Mat
import BIDMat.TMat
import BIDMat.IMat
import BIDMat.SciFunctions.variance
import BIDMat.MatFunctions.?
import BIDMat.MatFunctions.ones
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach.Learner
import BIDMach.models.Model
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.routing.sird

import scala.reflect.runtime.universe._
import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.ListBuffer


/**
  * Created by han on 11/30/16.
  */
class WebServerChannel(val learner: Learner) extends Learner.LearnerObserver {
  class StatFunction(var name: String, var code: String, var funcPointer: (Model, Array[Mat]) => Mat) {}

  var stats: Map[String, StatFunction] = Map("variance" -> new StatFunction(
    "variance", "", WebServerChannel.arraymean _));
  val interestingStats = List(
    ("Learner.Opts", learner.opts)
  )
  var server = LocalWebServer.mkNewServer(this)
  var prevPass = -1
  // server.startServer()

  def mkJson(msgType: String, content: String): String =
    s"""
       |{
       |  "msgType": "$msgType",
       |  "content": $content
       |}
     """.stripMargin

  def addNewFunction(requestJson: JsValue) = {
    val name = (requestJson \ "name").as[String]
    val code = (requestJson \ "code").as[String]
    val function = Eval.evaluateCodeToFunction(code)
    println(code)
    stats = stats + (name -> new StatFunction(name, code, function))
  }

  def pauseTraining(args: JsValue) = {
    learner.paused = args.as[Boolean]
  }

  def modifyParam(args: JsValue): Unit = {
    val key = (args \ "name").as[String]
    val value = (args \ "value").as[String]

    WebServerChannel.setValue(learner.opts, key, value.toDouble)
  }

  def handleRequest(requestJson: JsValue):String = {
    val methodName = (requestJson \ "methodName").as[String]
    val values = requestJson \ "content"
    methodName match {
      case "addFunction" =>
        addNewFunction(values.as[JsValue])
      case "pauseTraining" =>
        pauseTraining(values.as[JsValue])
      case "modifyParam" =>
        modifyParam(values.as[JsValue])
      case _ =>
        error("method not found")
    }
    ""
  }

  def computeStatsToJson(name:String, x: AnyRef): String = {
    val rm = scala.reflect.runtime.currentMirror
    val accessors = rm.classSymbol(x.getClass).toType.members.collect {
      case m: MethodSymbol if m.isGetter && m.isPublic => m
    }
    val instanceMirror = rm.reflect(x)
   // var result = JsObject(Seq(
    //  accessors.map(x => (x.toString, instanceMirror.reflectClass(x).apply()))
    //))
    var result = MMap[String, String]()
    for(acc <- accessors) {
      var value = instanceMirror.reflectMethod(acc).apply()
      var value2 = ""
      if (value != null) {
        value2 = value.toString
      }
      var key = acc.toString
      result += (key -> value2)
    }
    return mkJson(name, Json.stringify(Json.toJson(result)))
  }

  def pushOutStats() = {
    for ((key, obj) <- interestingStats) {
      if (obj != null) {
        val m2 = computeStatsToJson(key, obj)
        server.func(m2)
      }
    }
  }


  override def notify(ipass: Int, model: Model, minibatch: Array[Mat]): Unit = {
    if (server.func != null) {
      for ((name, f) <- stats) {
        val result = f.funcPointer(model, minibatch)
        if (ipass > prevPass) {
          val (sizes, data) = WebServerChannel.matToArr(result)
          val content = DataPointContent(name, ipass, sizes, data)
          val message = Message("data_point", content)
          server.func(Json.toJson(message).toString)
          pushOutStats()
        }
      }
      prevPass = ipass
    }
  }
}

object WebServerChannel {
  def matToArr(m: Mat): (Seq[Int], Seq[String]) = {
    val row = m.nrows
    val col = m.ncols
    val result = (for (i <- 0 until row;
                       j <- 0 until col) yield s"${m(i, IMat(j))}")
    (Seq(row, col), result)
  }

  def setValue(obj: AnyRef, name: String, value: Any): Unit = {
    obj.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(obj, value.asInstanceOf[AnyRef])
  }
  def allOnes(mats: Array[Mat]): Mat = {
    ones(1,1)
  }
  def arraymean(model: Model, minibatch: Array[Mat]): Mat = {
    var m:Mat = null
    for (i <- model.mats) {
       i match {
        case i:TMat => m = variance(i.tiles(0)(?))
        case i:Mat => m = variance(i(?))
      }
    }
    m
  }
}

