package BIDMach.ui

import BIDMach.ui.Message.{contentWrite, messageWrite}
import BIDMach.Learner
import BIDMat.{Mat,FMat,GMat,TMat,IMat,DMat}
import BIDMat.SciFunctions.variance
import BIDMat.MatFunctions.?
import BIDMat.MatFunctions.ones
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach.Learner
import BIDMach.models.Model
import BIDMach.networks.Net
import play.api.routing.sird
import play.api.libs.json._

import scala.reflect.runtime.universe._
import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.ListBuffer
import scala.io.Source
import java.io._
import java.util.Scanner

import scala.collection.mutable
import scala.tools.reflect.ToolBoxError


/**
  * Created by han on 11/30/16.
  */
class WebServerChannel(val learner: Learner, val debug:Boolean = false) extends Learner.LearnerObserver {

  class StatState(var maxSize: Int) {
    var stats: MMap[String, StatFunction] = MMap()
    var datapoints: mutable.Queue[MMap[String, (Mat, Int)]] = mutable.Queue()
    var currentStart = 0
    var currentTick = 0

    def load(fromFile: java.io.File):String = {
      val stringContent = new Scanner(
        fromFile).useDelimiter("\\Z").next()
      val content = (Json.parse(stringContent) \ "stats").as[Map[String, JsValue]]
      for ((key, value) <- content) {
        println(key)
        println(value)
        val name = (value \ "name").as[String]
        val code = (value \ "code").as[String]
        val theType = (value \ "type").as[String]
        val size = (value \ "size").as[Int]
        val ui = (value \ "ui").as[String]
        val funcPointer = Eval.evaluateCodeToFunction(code)
        val statFunc = StatFunction(name, code, size, theType, funcPointer, ui)
        stats += (key -> statFunc)
      }
      return stringContent
    }

    def save(toFile: java.io.File): Unit = {
      val writer = new PrintWriter(toFile)
      val content = Json.obj(
        "stats" -> Json.toJson(stats.toMap)
        // "datapoints" -> Json.toJson(
      ).toString

      writer.write(content)
      writer.close()
      println("I am here")
    }

    def getStatByName(name: String) = stats.get(name)
    def addFunction(name:String, code: String, theType: String): Unit = {
      val function = Eval.evaluateCodeToFunction(code)
      stats = stats + (name -> new StatFunction(name, code, 1, theType, function, ""))
    }
    def evaluateDatapoint(model: Model, minibatch: Array[Mat], learner: Learner):
        (MMap[String, (Mat, Int)], List[String]) = {

      var result: MMap[String, (Mat, Int)] = datapoints get currentTick match {
        case Some(a) => a
        case None => MMap()
      }
      var exceptions: List[String] = List()
      for ((name, f) <- stats) {
        if (debug) println(s"evaluating, $name")
        try {
          val newmat = f.funcPointer(model, minibatch, learner)
          result get name match {
            case Some((mat, count)) => result += (name -> (mat + newmat, count + 1))
            case None => result += (name -> (newmat, 1))
          }
        } catch {
          case throwable: Throwable =>
            stats -= name // get rid of it from the map
            val message = s"the chart ($name) had a runtime error: (${throwable.toString})"
            exceptions = exceptions :+ message
        }
      }

      // insert the result to the end
      if (datapoints.size > maxSize) {
        datapoints.dequeue()
        currentStart += 1
      }
      if (result.size > 0) {
        datapoints.enqueue(result)
      }
      (result, exceptions)
    }

    def getAggregatedDatapointAndIncrementTick(start: Int, end: Int): MMap[String, Mat] = {
      var result: MMap[String, Mat] = MMap()
      datapoints get currentTick match {
        case Some(datapoint) =>
          for ((name, mat_count) <- datapoint) {
            result += (name -> (mat_count._1 / mat_count._2))
          }
          currentTick += 1
        case None =>
          null
      }
      result
    }
  }

  var state: StatState = new StatState(10000)
  var AVG_MESSAGE_TIME_MILLIS = 1000 // one message to UI per every 200 millis
  var lastMessageTimeMillis:Long = 0 // last time a message is sent in epoch millis
  var stats = MMap[String, StatFunction]()
  val interestingStats = List(
    ("Learner.Opts", learner.opts)
  )
  var server = new VizWebServer(this)
  var prevUpdate = 0 // last time a data point is send to client
  var currentTick = 0
  // server.startServer()
  // loadAllMetricsFiles()
  
  def addNewFunction(requestJson: JsValue): (Boolean, String) = {
    val name = (requestJson \ "name").as[String]
    val code = (requestJson \ "code").as[String]
    val size = 1
    val theType = (requestJson \ "type").as[String]
    try {
      println(code)
      state.addFunction(name, code, theType)
      return (true, "")
    } catch {
      case e: ToolBoxError => return (false, e.getMessage())
    }
  }

  def pauseTraining(args: JsValue): (Boolean, String) = {
    println("pausing ", args)
    learner.paused = args.as[Boolean]
    (true, "")
  }

  def modifyParam(args: JsValue): (Boolean, String) = {
    for (a <- args.as[List[Map[String, String]]]) {
      val key = a("key")
      val value = a("value")
      println("changed value", key, value)
      WebServerChannel.setValue(learner.opts, key, value)
      println(learner.opts.what)
    }
    (true, "")
  }

  def requestParam(): (Boolean, String) = {
    var messages = new ListBuffer[Message]
    for ((key, obj) <- interestingStats) {
      if (obj != null) {
        val m2 = computeStatsToMessage(key, obj)
        messages += m2
      }
    }
    server.func(Json.toJson(messages).toString())
    (true, "")
  }

  def evaluateCommand(jsValue: JsValue): (Boolean, String) = {
    val code = (jsValue \ "code").as[String]
    try {      
      val func = Eval.evaluateCodeToCommand(code)
      val result = func(learner).toString
      return (true, result)
    } catch {
      case e: ToolBoxError => return (false, e.getMessage())
      case e: Throwable => return (false, e.toString())
    }
  }

  def getCodeSnippet(name: JsValue): (Boolean, String)= {
    val n = (name \ "name").as[String]
    state.getStatByName(n) match {
      case Some(a) => (true, Json.toJson(a).toString)
      case None => (false, "")
    }
  }
    
  def getModelGraph() = 
      learner.model match {
          case m:Net =>
              CodeHelpers.getModelGraph(learner)
              //(true,Json.toJson(m.layers.map(_.getClass.getSimpleName)).toString)
          case _ =>
              (true,"[\"layer1\",\"layer2\"]")
  }
  
  def handleRequest(requestJson: JsValue): CallbackMessage = {
    val methodName = (requestJson \ "methodName").as[String]
    val values = requestJson \ "content"
    println("HAN method name", methodName)
    val (status, message) = methodName match {
      case "addFunction" =>
         addNewFunction(values.as[JsValue])
      case "pauseTraining" =>
         pauseTraining(values.as[JsValue])
      case "modifyParam" =>
        modifyParam(values.as[JsValue])
      case "requestParam" =>
        requestParam()
      case "evaluateCommand" =>
        evaluateCommand(values.as[JsValue])
      case "getCode" =>
        getCodeSnippet(values.as[JsValue])
      case "saveMetrics" =>
        var name = values.as[String]
        var file = new File(name)
        state.save(file)
        (true, "")
      case "getModelGraph" =>
        getModelGraph()
      case "loadMetrics" =>
        var name = values.as[String]
        println("I am here 2", name)
        var file = new File(name)
        val result = state.load(file)
        (true, result)      
      /*
      case "getDataForTick" =>
        var start = (value.as[JsValue] \ "start").as[Int]
        var end = (value.as[JsValue] \ "start").as[Int]
        val result = state.getAggregatedDatapointAndIncrementTick(start, end)
        (true, Json.toJson(result.toMap).toString)
      */

    }
    return CallbackMessage("", status, message)
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
      if (filename(0) != '.') {
        val metricsCode = loadMetricsFile(WebServerChannel.metricLocation + filename)
        val metricsInfo = filename.split("\\$")
        val metricsName = metricsInfo(0)
        val metricsSize = metricsInfo(1).toInt
        val metricsType = metricsInfo(2)
        val metricsFunction = Eval.evaluateCodeToFunction(metricsCode)
        stats = stats + (metricsName -> new StatFunction(metricsName, metricsCode,
          metricsSize, metricsType, metricsFunction, ""))
      }
    }
  }

  override def notify(ipass: Int, model: Model, minibatch: Array[Mat]): Unit = {
    // if has passed enough time, fetch a collection of data points and send them out.
    var currentTime = System.currentTimeMillis()
    var messages = new ListBuffer[Message]
    if ((currentTime - lastMessageTimeMillis) > AVG_MESSAGE_TIME_MILLIS &&
      server.func != null) {
      // save data point into datas
      val (_, exceptions) = state.evaluateDatapoint(model, minibatch, learner)
    // always send out error messages
      for (exc <- exceptions) {
          val error = ErrorMessage(exc)
          messages += Message("error_message", error)
      }
      val results = state.getAggregatedDatapointAndIncrementTick(prevUpdate, currentTick)
      for ((name, mat) <- results) {
        val (sizes, data) = WebServerChannel.matToArr(mat)
        val content = DataPointContent(name, currentTick, sizes, data, "")
        val message = Message("data_point", content)
        messages += message
      }
      lastMessageTimeMillis = currentTime
      prevUpdate = currentTick
    }

    // send out message only when there is stuff to send
    if (messages.size > 0) {
      server.func(Json.toJson(messages).toString)
    }
    currentTick += 1
  }
}

object WebServerChannel {
  val metricLocation = "src/main/resources/metrics/"

  def matToArr(m: Mat): (Seq[Int], Seq[Float]) = {
    if (m == null) {
      return (Seq(0, 0), Seq())
    }
    val row = m.nrows
    val col = m.ncols
//    val result = (for (i <- 0 until row;
//                       j <- 0 until col) yield s"${m(i, IMat(j))}")
    val result = FMat(m).data
    (Seq(row, col), result)
  }

  def setValue(obj: AnyRef, name: String, value: String): Unit = {
    var targetMethod = obj.getClass.getMethods.find(_.getName == name).get
    var targetClass = targetMethod.getReturnType   
    var target = targetMethod.invoke(obj)
    var targetVal: AnyRef = null
    targetClass match {
      case _: Class[Float] => targetVal = value.toFloat.asInstanceOf[AnyRef]
      case _: Class[Double] => targetVal = value.toDouble.asInstanceOf[AnyRef]
      case _: Class[Int]  => targetVal = value.toInt.asInstanceOf[AnyRef]
      case _: Class[Boolean]  => targetVal = value.toBoolean.asInstanceOf[AnyRef]
    }
    target match {
        case m:FMat => m(0) = value.toFloat
        case m:IMat => m(0) = value.toInt
        case m:DMat => m(0) = value.toDouble
        case _ => obj.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(obj, targetVal)
    }
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

