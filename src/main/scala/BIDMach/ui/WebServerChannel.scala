package BIDMach.ui
import BIDMach.models.Model
import BIDMat.{Mat, TMat}
import play.core.server.NettyServer
import play.api.routing.sird.GET
import play.api.routing.sird._
import play.api.routing._
import play.api.mvc._
import play.api.mvc.Action
import play.api.mvc.Results
import play.api.mvc.WebSocket

import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox
import java.io.File

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
// import play.core.server.ServerProcess._
import BIDMach.models.Model
import BIDMat.Mat
import BIDMat.TMat
import BIDMat.SciFunctions.variance
import BIDMat.MatFunctions.ones

/**
  * Created by han on 11/30/16.
  */
class WebServerChannel extends NetSink.Channel {

  var funcList: List[Function[Array[Mat], Mat]] = List() // Model=>Mat
  var server = new TestServer
  // server.startServer()
  funcList :+ (WebServerChannel.allOnes _)

  def convertToString(ipass: Int, result: Mat) =
  //for now assumes 1x1
    s"""
       | {
       |     "ipass": "$ipass",
       |     "value": "$result(0, 0)"
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

  def createAndStartServer() = {
    val server = NettyServer.fromRouter() {
      case GET(p"/") => Action {
        Results.Ok(s"hello world")
      }
    }
  }

  def evaluateCodeToFunction(code: String): Function[Array[Mat], Mat] = {
    val completeCode = codeTemplate.format("Classname", code, "Classname")
    println(completeCode)
    val classloader = ClassLoader.getSystemClassLoader
    // val classloader = getClass.getClassLoader
    // val classloader = new URLClassLoader
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
    for (f <- funcList) {
      val result = f(mats)
      if (server.func != null) {
        server.func(convertToString(ipass, result))
      }
    }
  }
}

object WebServerChannel {
  def allOnes(mats: Array[Mat]): Mat = {
    ones(1,1)
  }
}

