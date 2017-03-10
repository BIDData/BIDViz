package BIDMach.ui

import BIDMach.Learner
import BIDMat.{FMat, GMat, Mat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._

/**
  * Created by han on 1/29/17.
  *  Helpers to user input code that used to create
  *  charts
  */

object CodeHelpers {

  def logLikelihood(learner: Learner):Mat = {
    val len = learner.reslist.length
    val istart = if (learner.opts.cumScore == 0) learner.lasti else
      {if (learner.opts.cumScore == 1) 0 else if (learner.opts.cumScore == 2) len/2 else 3*len/4};
    var i = 0
    var sum = 0.0;
    for (scoremat <- learner.reslist) {
      if (i >= istart) sum += mean(scoremat(?,0)).v
      i += 1
    }
    var mat = ones(1,1)
    mat(0, 0) = (sum/(len - istart))
    mat
  }

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
