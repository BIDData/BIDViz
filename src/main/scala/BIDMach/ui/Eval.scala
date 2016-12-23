package BIDMach.ui


import java.io.File
import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import BIDMat.Mat

/**
  * Created by han on 12/22/16.
  */
object Eval {
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
    val cm = universe.runtimeMirror(classloader)
    val tb = cm.mkToolBox()
    val clazz = tb.compile(tb.parse(completeCode))().asInstanceOf[Class[_]]
    val ctor = clazz.getDeclaredConstructors()(0)
    val instance = ctor.newInstance().asInstanceOf[Function[Array[Mat], Mat]]
    return instance
  }
}
