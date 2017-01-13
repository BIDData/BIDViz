package BIDMach.ui

import BIDMach.Learner
import BIDMat.SciFunctions.variance
import BIDMat.{FMat, Mat, TMat}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.routing.sird

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
// import play.core.server.ServerProcess._
import BIDMat.Mat
import BIDMat.TMat
import BIDMat.SciFunctions.variance
import BIDMat.MatFunctions.{ones, zeros}
import BIDMat.MatFunctions.?
import BIDMat.MatFunctions.ones
import java.io.File
import javax.script.ScriptEngineManager

import scala.reflect.runtime.universe._
import scala.collection.mutable.{Map => MMap}


/**
  * Created by han on 11/30/16.
  */
class WebServerChannel(val learner: Learner) extends NetSink.Channel {
  class StatFunction(var name: String, var code: String, var funcPointer: Array[Mat] => Mat) {}

  var stats: Map[String, StatFunction] = Map("variance" -> new StatFunction(
    "variance", "", WebServerChannel.arraymean _));
  var funcList: ListBuffer[Array[Mat] => Mat]
    = ListBuffer(WebServerChannel.arraymean _) // Model=>Mat
  val interestingStats = List(
    ("Model.Opts", learner.mopts),
    ("Learner.Opts", learner.opts),
    ("Mixin.Opts", learner.ropts),
    ("Updater.Opts", learner.uopts),
    ("DataSource.Opts", learner.dopts)
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
  def convertToString(ipass: Int, result: Mat, name: String): String =
  //for now assumes 1x1
    mkJson("data_point", s"""
       | {
       |     "name": "$name",
       |     "ipass": "$ipass",
       |     "value": "$result"
       | }
      """.stripMargin)

  def addNewFunction(requestJson: JsValue) = {
    val name = (requestJson \ "name").as[String]
    val code = (requestJson \ "code").as[String]
    val function = Eval.evaluateCodeToFunction(code)
    println(code)
    funcList.append(function)
    stats = stats + (name -> new StatFunction(name, code, function))
  }

  def pauseTraining(args: JsValue) = {
    learner.paused = args.as[Boolean]
    println("lloook here", learner.paused)
  }

  def modifyParam(args: JsValue): Unit = {
    val key = (args \ "name").as[String]
    val value = (args \ "value").as[String]
    WebServerChannel.setValue(learner.opts, key, value)
  }

  def handleRequest(requestJson: JsValue) = {
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


  override def push(ipass: Int, mats: Array[Mat]): Unit = {
    if (server.func != null) {
      for ((name, f) <- stats) {
        val result = f.funcPointer(mats)
        if (ipass > prevPass) {
          val message = convertToString(ipass, result, name)
          server.func(message)
          pushOutStats()
        }
      }
      prevPass = ipass
    }
  }
}

object WebServerChannel {
  def setValue(obj: AnyRef, name: String, value: Any): Unit = {
    obj.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(obj, value.asInstanceOf[AnyRef])
  }
  def allOnes(mats: Array[Mat]): Mat = {
    ones(1,1)
  }
  def arraymean(mats: Array[Mat]): Mat = {
    var m:Mat = null
    for (i <- mats) {
       i match {
        case i:TMat => m = variance(i.tiles(0)(?))
        case i:Mat => m = variance(i(?))
      }
    }
    m
  }
}

