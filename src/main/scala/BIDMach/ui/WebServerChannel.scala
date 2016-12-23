package BIDMach.ui

import BIDMat.SciFunctions.variance
import BIDMat.{FMat, Mat, TMat}
import play.api.libs.json.JsValue
import play.api.routing.sird

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


/**
  * Created by han on 11/30/16.
  */
class WebServerChannel extends NetSink.Channel {
  var funcList: ListBuffer[Function[Array[Mat], Mat]]
    = ListBuffer(WebServerChannel.arraymean _) // Model=>Mat
  var server = LocalWebServer.mkNewServer(this)
  var prevPass = -1
  // server.startServer()

  def convertToString(ipass: Int, result: Mat) =
  //for now assumes 1x1
    s"""
       | {
       |     "ipass": "$ipass",
       |     "value": "$result"
       | }
      """.stripMargin


  def handleRequest(requestJson: JsValue) = {
    val name = (requestJson \ "name").as[String]
    val code = (requestJson \ "code").as[String]
    val function = Eval.evaluateCodeToFunction(code)
    println(code)

    funcList.append(function)
  }

  override def push(ipass: Int, mats: Array[Mat]): Unit = {
    println("push is called", server.func, funcList.length)
    for (f <- funcList) {
      val result = f(mats)
      if (server.func != null) {
        println("server.func is not null", ipass)
        if (ipass > prevPass) {
          server.func(convertToString(ipass, result))
          prevPass = ipass
        }
      }
    }
  }
}

object WebServerChannel {
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

