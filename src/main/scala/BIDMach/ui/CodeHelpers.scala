package BIDMach.ui

import BIDMat.{FMat, GMat, Mat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._

/**
  * Created by han on 1/29/17.
  *  Helpers to user input code that used to create
  *  charts
  */

object CodeHelpers {

  def toHistogram(input: Mat, buckets:Int): Mat = {
    var result = zeros(buckets, 2)
    var maximun:Float = maxi(input(?), 1) match {
      case i: GMat => i (0, 0)
      case i: Mat => i.asInstanceOf[FMat] (0, 0)
    }
    var minimun:Float = mini(input(?), 1) match {
      case i: GMat => i (0, 0)
      case i: Mat => i.asInstanceOf[FMat] (0, 0)
    }
    if (maximun > minimun) {
      var size = (maximun - minimun) / buckets
      var f:FMat = FMat(input(?))
      for (elm <- 0 until f.size) {
        var current = f(elm)
        var position = ((current - minimun) / size).toInt
        if (position >= result.nrows) {
          position = result.nrows - 1
        }
        result(position, 0) = position * size + minimun
        result(position, 1) += 1
      }
    }
    return result
  }
}
