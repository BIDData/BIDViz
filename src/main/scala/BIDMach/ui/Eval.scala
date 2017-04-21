package BIDMach.ui


import BIDMach.models.Model
import java.io.File

import scala.reflect.runtime.universe
import scala.tools.reflect.{ToolBox, ToolBoxError}
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import BIDMat.Mat
import BIDMach.Learner

/**
  * Created by han on 12/22/16.
  */
object Eval {
  val codeTemplate =
    """
      |import BIDMach.models.Model
      |import BIDMat.{Mat,FMat,GMat}
      |import BIDMat.TMat
      |import BIDMat.MatFunctions._
      |import BIDMat.SciFunctions._
      |import BIDMat.Solvers._
      |import BIDMat.Plotting._
      |import BIDMach.Learner
      |import BIDMach.networks.Net
      |import BIDMach.ui.CodeHelpers._
      | class %s extends Function3[Model, Array[Mat], Learner, Mat] {
      |   var temp: AnyRef = null
      |   override def apply(model: Model, minibatch: Array[Mat], learner: Learner): Mat = {
      |     %s
      |   }
      | }
      | scala.reflect.classTag[%s].runtimeClass
    """.stripMargin

  val commandTemplate =
    """
      |import BIDMach.models.Model
      |import BIDMat.Mat
      |import BIDMat.MatFunctions._
      |import BIDMach.Learner
      |import BIDMach.ui.CodeHelpers._
      | class %s extends Function[Learner, Any] {
      |   override def apply(learner: Learner): Any = {
      |     %s
      |   }
      | }
      | scala.reflect.classTag[%s].runtimeClass
    """.stripMargin
  def getFunctor[T](template: String): T = {
    var classloader = new URLClassLoader(
      new File("lib").listFiles.filter(_.getName.endsWith(".jar")).map(_.toURL).toSeq,
      this.getClass.getClassLoader
    )
    val cm = universe.runtimeMirror(classloader)
    val tb = cm.mkToolBox()
    val clazz = tb.compile(tb.parse(template))().asInstanceOf[Class[_]]
    val ctor = clazz.getDeclaredConstructors()(0)
    val instance = ctor.newInstance().asInstanceOf[T]
    return instance
  }

  def evaluateCodeToFunction(code: String): (Model, Array[Mat], Learner) => Mat = {
    val completeCode = codeTemplate.format("Classname", code, "Classname")
    return getFunctor[(Model, Array[Mat], Learner) => Mat](completeCode)
  }

  def evaluateCodeToCommand(code: String): (Learner => AnyRef) = {
    val completeCode = commandTemplate.format("Classname", code, "Classname")
    return getFunctor[(Learner) => AnyRef](completeCode)
  }

}
