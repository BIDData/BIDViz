package BIDMach.ui

import BIDMat.SciFunctions.variance
import BIDMat.{FMat, Mat, TMat}
import play.api.routing.sird

//import scala.reflect.runtime.universe
//import scala.tools.reflect.ToolBox
//import java.io.File

// import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
// import play.core.server.ServerProcess._
import BIDMat.Mat
import BIDMat.TMat
import BIDMat.SciFunctions.variance
import BIDMat.MatFunctions.{ones, zeros}
import BIDMat.MatFunctions.?

/**
  * Created by han on 11/30/16.
  */
class WebServerChannel extends NetSink.Channel {

  var funcList: List[Function[Array[Mat], Mat]]
    = List( WebServerChannel.arraymean _) // Model=>Mat
  var server = LocalWebServer.mkNewServer()
  // server.startServer()

  def convertToString(ipass: Int, result: Mat) =
  //for now assumes 1x1
    s"""
       | {
       |     "ipass": "$ipass",
       |     "value": "$result"
       | }
      """.stripMargin

  val codeTemplate =
    """
      |import BIDMach.models.Model
      |import BIDMat.Mat
      |import BIDMat.MatFunctions._
      | class %s extends Function[Model, Mat] {
      |   override def apply(mats: Array[Mat]): Mat = {
      |     %s
      |   }
      | }
      | scala.reflect.classTag[%s].runtimeClass
    """.stripMargin

//  def evaluateCodeToFunction(code: String): Function[Array[Mat], Mat] = {
//    val completeCode = codeTemplate.format("Classname", code, "Classname")
//    println(completeCode)
//    val classloader = ClassLoader.getSystemClassLoader
//    // val classloader = getClass.getClassLoader
//    // val classloader = new URLClassLoader
//    val cm = universe.runtimeMirror(classloader)
//    val tb = cm.mkToolBox()
//    val clazz = tb.compile(tb.parse(completeCode))().asInstanceOf[Class[_]]
//    val ctor = clazz.getDeclaredConstructors()(0)
//    val instance = ctor.newInstance().asInstanceOf[Function[Array[Mat], Mat]]
//    return instance
//  }

  def fixClassPath() = {

  }

  override def push(ipass: Int, mats: Array[Mat]): Unit = {
    println("push is called", server.func, funcList.length)
    for (f <- funcList) {
      val result = f(mats)
      if (server.func != null) {
        println("server.func is not null", ipass)
        server.func(convertToString(ipass, result))
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
       m = variance(i(?))
    }
    m
  }
}

