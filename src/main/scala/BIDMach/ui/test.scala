package BIDMach.ui
import scala.tools.reflect.ToolBoxError

/**
  * Created by claireliu on 1/22/17.
  */

object Test {
  var name = "batchSize"
  var value = "2000"
  def testError:(Int, String) = {
    try {
      var code = "ones(1,1)."
      val function = Eval.evaluateCodeToFunction(code)
      println(code)
      return (0, "Code pushed is valid.")
    } catch {
      case e: ToolBoxError => return (1, e.getMessage())
    }
  }

}

