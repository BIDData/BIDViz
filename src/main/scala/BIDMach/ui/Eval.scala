package BIDMach.ui


import java.io.File

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
      |import BIDMat.MatFunctions._
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

  def evaluateCodeToFunction(code: String): (Model, Array[Mat]) => Mat = {

    var classloader = new URLClassLoader(
      new File("lib").listFiles.filter(_.getName.endsWith(".jar")).map(_.toURL).toSeq,
      this.getClass.getClassLoader
    )
    val completeCode = codeTemplate.format("Classname", code, "Classname")
    val cm = universe.runtimeMirror(classloader)
    val tb = cm.mkToolBox()
    val clazz = tb.compile(tb.parse(completeCode))().asInstanceOf[Class[_]]
    val ctor = clazz.getDeclaredConstructors()(0)
    val instance = ctor.newInstance().asInstanceOf[(Model, Array[Mat]) => Mat]
    return instance
  }

  def evaluatePar(parName: String, parValue: String): Learner.Options => Unit = {

    var classloader = new URLClassLoader(
      new File("lib").listFiles.filter(_.getName.endsWith(".jar")).map(_.toURL).toSeq,
      this.getClass.getClassLoader
    )
    val completeCode = varTemplate.format("Classname", parName, parValue, "Classname")
    val cm = universe.runtimeMirror(classloader)
    val tb = cm.mkToolBox()
    val clazz = tb.compile(tb.parse(completeCode))().asInstanceOf[Class[_]]
    val ctor = clazz.getDeclaredConstructors()(0)
    val instance = ctor.newInstance().asInstanceOf[Learner.Options => Unit]
    return instance
  }
}
