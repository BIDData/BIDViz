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
import scala.io.Source
import java.io._
import scala.tools.reflect.ToolBoxError


/**
  * Created by han on 11/30/16.
  */
class WebServerChannel(val learner: Learner) extends Learner.LearnerObserver {
  var stats = MMap[String, StatFunction]()
  val interestingStats = List(
    ("Learner.Opts", learner.opts)
  )
  var server = LocalWebServer.mkNewServer(this)
  var prevPass = -1
  // server.startServer()
  loadAllMetricsFiles()

  def mkJson(msgType: String, content: String): String =
    s"""
       |{
       |  "msgType": "$msgType",
       |  "content": $content
       |}
     """.stripMargin

  def addNewFunction(requestJson: JsValue): (Int, String) = {
    val name = (requestJson \ "name").as[String]
    val code = (requestJson \ "code").as[String]
    val size = 1
    val theType = (requestJson \ "type").as[String]
    try {
      val function = Eval.evaluateCodeToFunction(code)
      println(code)
      stats = stats + (name -> new StatFunction(name, code, size, theType, function))
      return (0, "Code pushed is valid.")
    } catch {
      case e: ToolBoxError => return (1, e.getMessage())
    }
  }

  def pauseTraining(args: JsValue) = {
    learner.paused = args.as[Boolean]
  }

  def modifyParam(args: JsValue): Unit = {
    val key = (args \ "name").as[String]
    val value = (args \ "value").as[String]
    WebServerChannel.setValue(learner.opts, key, value)
  }

  def handleRequest(requestJson: JsValue): (Int, String) = {
    val methodName = (requestJson \ "methodName").as[String]
    val values = requestJson \ "content"
    var status: Int = 0
    var msg: String = "good"
    methodName match {
      case "addFunction" =>
        var result = addNewFunction(values.as[JsValue])
        status = result._1
        msg = result._2
      case "pauseTraining" =>
        pauseTraining(values.as[JsValue])
      case "modifyParam" =>
        modifyParam(values.as[JsValue])
      case _ =>
        error("method not found")
    }
    (status, msg)
  }

  def computeStatsToMessage(name:String, x: AnyRef): Message = {
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
    var map = Map(result.toList: _*)
    val message = ParameterContent(map)
    return Message("parameters", message)
  }

  def pushOutStats() = {
  }

  def saveMetricsFile(filename: String, codefile: String) {
    val writer = new PrintWriter(new File(WebServerChannel.metricLocation + filename))
    writer.write(codefile)
    writer.close()
  }

  def loadMetricsFile(filename: String): String = {
    val source = scala.io.Source.fromFile(filename)
    val lines = try source.mkString finally source.close()
    return lines
  }

  def getListOfFiles(dir: String):List[String] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.map(_.getName).toList
    } else {
      List[String]()
    }
  }

  def loadAllMetricsFiles() = {
    val listOfFiles = getListOfFiles(WebServerChannel.metricLocation)
    for (filename <- listOfFiles) {
      val metricsCode = loadMetricsFile(WebServerChannel.metricLocation + filename)
      val metricsInfo = filename.split("\\$")
      val metricsName = metricsInfo(0)
      val metricsSize = metricsInfo(1).toInt
      val metricsType = metricsInfo(2)
      val metricsFunction = Eval.evaluateCodeToFunction(metricsCode)
      stats = stats + (metricsName -> new StatFunction(metricsName, metricsCode,
        metricsSize, metricsType, metricsFunction))
    }
  }

  override def notify(ipass: Int, model: Model, minibatch: Array[Mat]): Unit = {
    var messages = new ListBuffer[Message]
    if (server.func != null) {
      for ((name, f) <- stats) {
        println(s"evaluating, $name")
        val result = f.funcPointer(model, minibatch)
        if (ipass > prevPass) {
          val (sizes, data) = WebServerChannel.matToArr(result)
          val content = DataPointContent(name, ipass, sizes, data, f.theType)
          val message = Message("data_point", content)
          messages += message
        }
      }
      for ((key, obj) <- interestingStats) {
        if (obj != null) {
          val m2 = computeStatsToMessage(key, obj)
          messages += m2
        }
      }
      server.func(Json.toJson(messages).toString)
      prevPass = ipass
    }
  }
}

object WebServerChannel {
  val metricLocation = "src/main/resources/metrics/"

  def matToArr(m: Mat): (Seq[Int], Seq[String]) = {
    if (m == null) {
      return (Seq(0, 0), Seq())
    }
    val row = m.nrows
    val col = m.ncols
    val result = (for (i <- 0 until row;
                       j <- 0 until col) yield s"${m(i, IMat(j))}")
    (Seq(row, col), result)
  }

  def setValue(obj: AnyRef, name: String, value: String): Unit = {
    var targetClass = obj.getClass.getMethods.find(_.getName == name).get.getReturnType
    var targetVal: AnyRef = null
    targetClass match {
      case _: Class[Float] => targetVal = value.toFloat.asInstanceOf[AnyRef]
      case _: Class[Double] => targetVal = value.toDouble.asInstanceOf[AnyRef]
      case _: Class[Int]  => targetVal = value.toInt.asInstanceOf[AnyRef]
      case _: Class[Boolean]  => targetVal = value.toBoolean.asInstanceOf[AnyRef]
    }
    obj.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(obj, targetVal)
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

