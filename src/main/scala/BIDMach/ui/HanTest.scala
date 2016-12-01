package BIDMach.ui

/**
  * Created by han on 11/30/16.
  */
object HanTest {
  def main(args: Array[String]) = {
    var x = new WebServerChannel
    val f = x.evaluateCodeToFunction("ones(4,4)")
    println(f(null))
  }
}
