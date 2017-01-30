# BIDMach_Viz

### Add interactivity to existing BIDMach script

This part assumes that you already have a script for BIDMach. We will use kmeans.ssc 
in this repository as example.

###<b>Steps:</b>
#### Use an alternative version BIDMach that exposes a field in Learner.Options for datasink
Please clone and build BIDMach from this repository by using following commands

```
  git clone https://github.com/qihqi/BIDMach
  cd BIDMach
  mvn package install 
```

If you get compile errors you might need to rebuild the latest version of BIDMat first, 
with these commands 

```
  cd ..
  git clone https://github.com/biddata/BIDMat
  cd BIDMat
  mvn package install 
```

Then rerun the previously failed commands

#### Clone this repository and build it 
```
  cd ..
  git clone https://github.com/qihqi/BIDMach_Viz
  cd BIDMach_Viz
  mvn package
```
#### Modify the script to add WebDataSink

Here we use the kmeans.ssc script included in BIDMach_Viz as example.

Before the script look like this:
```
import BIDMach.models.KMeans.MatOptions

import BIDMat.{Mat,SBMat,CMat,DMat,FMat,IMat,HMat,GMat,GIMat,GSMat,SMat,SDMat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach.datasources._
import BIDMach.datasinks._
import BIDMach.updaters._
import BIDMach._
import BIDMach.ui.NetSink

val mat0 = rand(100, 100000)
val opts = new MatOptions
opts.dim = 256
opts.batchSize = math.min(100000, mat0.ncols/30 + 1)
opts.npasses = 1000
val nn = new Learner(
        new MatSource(Array(mat0:Mat), opts), 
        new KMeans(opts), 
        null,
        new Batch(opts), 
        null, 
        opts)
nn.train
```
New we want add WebServerChannel as learner listener to the learner before the line nn.train

```
import BIDMach.ui.WebServerChannel

nn.opts.observer = new WebServerChannel(nn)
```
WebServer takes a Learner instance as constructor argument.
Save the file. Now we can run it by first running a sbt console using 
```sbt console```
Then load the file using 
```:load kmeans.ssc```


#### Interactive Machine Learning toolkit based on BIDMach

#### Install and build

This toolkit uses [play] web framework and it requires Java 8. We use [maven] for package management. You can modify [pom.xml] to configure the build process. 

We support both CUDA 7.0, 7.5 and 8.0. You will need to change the project version in pom.xml to 1.1.0-cuda7.0  1.1.0-cuda7.5 or 1.1.1-cuda8.0 base on your preference. 

To compile the project, use:
<pre>
git clone https://github.com/BIDData/BIDMach_Viz.git
mvn compile
</pre>

To generate the package and pull all the jars into the lib folder, use:
<pre>
mvn package
</pre>

After you get all the jars, the web server can be started using:
<pre>
./sbt -J-Xmx32g run
</pre>

And then select the program you would like to run.

[play]: https://www.playframework.com/
[maven]: https://maven.apache.org/
[pom.xml]: https://github.com/BIDData/BIDMach_Viz/blob/master/pom.xml







