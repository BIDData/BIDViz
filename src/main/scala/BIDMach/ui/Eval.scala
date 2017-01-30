package BIDMach.ui


import java.io.File
import java.lang.reflect.Constructor

import BIDMach.models.Model

import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox
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
      |import BIDMat.Mat
      |import BIDMat.TMat
      |import BIDMat.MatFunctions._
      |import BIDMat.SciFunctions._
      |import BIDMat.Solvers._
      |import BIDMat.Plotting._
      |import BIDMach.Learner
      |import BIDMach.networks.Net
      |import BIDMach.ui.CodeHelpers._
      | class %s extends Function2[Model, Array[Mat], Mat] {
      |   override def apply(model: Model, minibatch: Array[Mat]): Mat = {
      |     %s
      |   }
      | }
      | scala.reflect.classTag[%s].runtimeClass
    """.stripMargin

  val varTemplate =
    """
      |import BIDMach.models.Model
      |import BIDMat.Mat
      |import BIDMat.MatFunctions._
      |import BIDMach.Learner
      | class %s extends Function[Learner.Options, Unit] {
      |   override def apply(opts: Learner.Options): Unit = {
      |     opts.%s = %s
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

  def evaluateCodeToFunction(code: String): (Model, Array[Mat]) => Mat = {
    val completeCode = codeTemplate.format("Classname", code, "Classname")
    return getFunctor[(Model, Array[Mat]) => Mat](completeCode)
  }

  def evaluatePar(parName: String, parValue: String): Learner.Options => Unit = {
    val completeCode = varTemplate.format("Classname", parName, parValue, "Classname")
    return getFunctor[Learner.Options => Unit](completeCode)
  }
}
