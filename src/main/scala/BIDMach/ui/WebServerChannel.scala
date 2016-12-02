package BIDMach.ui

import BIDMat.SciFunctions.variance
import BIDMat.{FMat, Mat, TMat}
import play.api.routing.sird

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

  var funcList: List[Function[Array[Mat], Mat]]
    = List( WebServerChannel.arraymean _) // Model=>Mat
  var server = LocalWebServer.mkNewServer()
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

  val codeTemplate =
    """
      |import BIDMach.models.Model
      |import BIDMat.Mat
      |import BIDMat.MatFunctions._
      | class %s extends Function[Array[Mat], Mat] {
      |   override def apply(mats: Array[Mat]): Mat = {
      |     %s
      |   }
      | }
      | scala.reflect.classTag[%s].runtimeClass
    """.stripMargin

  def evaluateCodeToFunction(code: String): Function[Array[Mat], Mat] = {

    var classloader = new URLClassLoader(
      new File("lib").listFiles.filter(_.getName.endsWith(".jar")).map(_.toURL).toSeq,
      this.getClass.getClassLoader
    )
    val completeCode = codeTemplate.format("Classname", code, "Classname")
   // println(completeCode)
    // val classloader = ClassLoader.getSystemClassLoader
    //println(classloader)
    // val classloader = getClass.getClassLoader
    // val classloader = new URLClassLoader
    //var classLoader = new URLClassLoader(Array(new File("BIDMachViz-1.1.0-cuda7.5.jar").toURI.toURL),
//                          this.getClass.getClassLoader)
    val cm = universe.runtimeMirror(classloader)
    val tb = cm.mkToolBox()
    val clazz = tb.compile(tb.parse(completeCode))().asInstanceOf[Class[_]]
    val ctor = clazz.getDeclaredConstructors()(0)
    val instance = ctor.newInstance().asInstanceOf[Function[Array[Mat], Mat]]
    return instance
  }

  def fixClassPath() = {

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

