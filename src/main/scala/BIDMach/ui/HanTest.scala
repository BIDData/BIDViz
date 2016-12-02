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
  def make_nn(mdir: String): (Learner, FDSopts) = {
    // Net.learnerX(mdir+"trainsortedx%02d.smat.lz4",mdir+"trainlabel%02d.fmat.lz4");
    println(Mat.checkMKL(false))
    println(Mat.checkCUDA)

    var x = List(FileSource.simpleEnum(mdir+"trainsortedx%02d.smat.lz4",1,0),
      FileSource.simpleEnum(mdir+"trainlabel%02d.fmat.lz4",1,0))

    val opts = new FDSopts
    opts.fnames = x
    opts.batchSize = 100000;
    opts.eltsPerSample = 500;
    val ds = new FileSource(opts);
    // val net = dnodes(3, 0, 1f, opts.targmap.nrows, opts)                   // default to a 3-node network
    var datasink = new NetSink()
    datasink.opts.names = Array("", "")
    datasink.opts.channel = new WebServerChannel
    val nn = new Learner(ds,
      new Net(opts),
      null,
      null,
      datasink, // datasink
      opts)
    (nn, opts)
  }

  def main(args:Array[String]): Unit = {

    val mdir = "../criteo/"
    // val (nn,opts) = make_nn(mdir)
    val (nn, opts) = Net.learnerX(mdir+"trainsortedx%02d.smat.lz4", mdir+"trainlabel%02d.fmat.lz4")
    opts.nend = 1
    opts.batchSize= 100
    opts.npasses = 1
    opts.lrate = 0.01f
    opts.texp = 0.3f
    opts.pstep = 0.001f

    opts.aopts = opts
    //opts.reg1weight = 0.0001
    //opts.hasBias = true
    opts.links = iones(1,1);
    opts.nweight = 1e-4f
    opts.lookahead = 0
    opts.autoReset = false

    val tshape = 0.25f
    val shape = irow(200,120,80,50,1)
    opts.tmatShape = Net.powerShape(tshape)_;
    opts.nodeset = Net.powerNet(shape,opts,0,2);
    opts.what
    println(tshape.toString)
    println(shape.toString)

    val model = nn.model.asInstanceOf[Net]
    nn.train
  }
}