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
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
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
  val AVG_MESSAGE_TIME_MILLIS = 500 // one message to UI per every 200 millis
  var lastMessageTimeMillis:Long = 0 // last time a message is sent in epoch millis
  var stats = MMap[String, StatFunction]()
  val interestingStats = List(
    ("Learner.Opts", learner.opts)
  )
  var server = new VizWebServer(this)
  var prevPass = 0
  // server.startServer()
  loadAllMetricsFiles()

  def addNewFunction(requestJson: JsValue): String = {
    val name = (requestJson \ "name").as[String]
    val code = (requestJson \ "code").as[String]
    val size = 1
    val theType = (requestJson \ "type").as[String]
    try {
      val function = Eval.evaluateCodeToFunction(code)
      println(code)
      stats = stats + (name -> new StatFunction(name, code, size, theType, function))
      saveMetricsFile(name+"$"+size+"$"+theType, code)
      return null
    } catch {
      case e: ToolBoxError => return e.getMessage()
    }
  }

  def pauseTraining(args: JsValue) = {
    learner.paused = args.as[Boolean]
  }

  def modifyParam(args: JsValue): Unit = {
    for (a <- args.as[List[Map[String, String]]]) {
      val key = a("key")
      val value = a("value")
      println("changed value", key, value)
      WebServerChannel.setValue(learner.opts, key, value)
      println(learner.opts.what)
    }
  }

  def requestParam() = {
    var messages = new ListBuffer[Message]
    for ((key, obj) <- interestingStats) {
      if (obj != null) {
        val m2 = computeStatsToMessage(key, obj)
        messages += m2
      }
    }
    server.func(Json.toJson(messages).toString())
  }

  def handleRequest(requestJson: JsValue): CallbackMessage = {
    val methodName = (requestJson \ "methodName").as[String]
    val values = requestJson \ "content"
    var message: String = null
    methodName match {
      case "addFunction" =>
        message = addNewFunction(values.as[JsValue])
      case "pauseTraining" =>
        pauseTraining(values.as[JsValue])
      case "modifyParam" =>
        modifyParam(values.as[JsValue])
      case "requestParam" =>
        requestParam()
      case _ =>

    }
    if (message == null) {
      return CallbackMessage("", true, "")
    } else {
      return CallbackMessage("", false, message)
    }

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
      var key = acc.toString().replace("method ", "");
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
    var currentTime = System.currentTimeMillis()
    prevPass += 1
    println("time difference", currentTime - lastMessageTimeMillis, AVG_MESSAGE_TIME_MILLIS)
    if ((currentTime - lastMessageTimeMillis)  > AVG_MESSAGE_TIME_MILLIS) {
      if (server.func != null) {
        for ((name, f) <- stats) {
          println(s"evaluating, $name")
          var result:Mat = null
          var exception:Throwable = null
          try {
            result = f.funcPointer(model, minibatch)
            val (sizes, data) = WebServerChannel.matToArr(result)
            val content = DataPointContent(name, prevPass, sizes, data, f.theType)
            val message = Message("data_point", content)
            messages += message
          } catch {
            case throwable: Throwable =>
              exception = throwable
              stats -= name // get rid of it from the map
              val message = s"the chart ($name) had a runtime error: (${throwable.toString})"
              val error = ErrorMessage(message)
              messages += Message("error_message", error)
          }
        }
        server.func(Json.toJson(messages).toString)
        lastMessageTimeMillis = currentTime
      }
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

