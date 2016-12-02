package BIDMach.ui

import BIDMat.{CMat,CSMat,DMat,Dict,FMat,FND,GMat,GDMat,GIMat,GLMat,GSMat,GSDMat,GND,HMat,IDict,Image,IMat,LMat,Mat,SMat,SBMat,SDMat,TMat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMat.Solvers._
import BIDMat.Plotting._
import BIDMach.Learner
import BIDMach.models.{Click,FM,GLM,KMeans,KMeansw,LDA,LDAgibbs,Model,NMF,SFA,RandomForest,SVD}
import BIDMach.networks.{Net}
import BIDMach.datasources.{DataSource,MatSource,FileSource,SFileSource}
import BIDMach.datasinks.{DataSink,MatSink}
import BIDMach.mixins.{CosineSim,Perplexity,Top,L1Regularizer,L2Regularizer}
import BIDMach.updaters.{ADAGrad,Batch,BatchNorm,Grad,IncMult,IncNorm,Telescoping}
import BIDMach.causal.{IPTW}
import BIDMach.networks.Net.FDSopts
import BIDMat.TMat

object HanTest {

  def main(args:Array[String]): Unit = {
    var datasink = new NetSink()
    datasink.opts.names = Array("name1", "name2")
    datasink.opts.channel = new WebServerChannel
    datasink.opts.ofcols = 2000
    System.in.read()
    val mat0 = rand(100, 100000)
    val (nn, opts) = KMeans.learner(mat0)
    nn.opts.publishSink = datasink
    nn.train
  }
}