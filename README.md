# BIDViz

### Add interactivity to existing BIDMach script

This We will use "kmeans.ssc" included in the repository as example file. You can 
also use the same procedure for other BIDMach scripts that you might have.

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

The script will start running, eventually you will see this log

```
23:49:37.420 [run-main-0] INFO  play.core.server.NettyServer - Listening for HTTP on /0:0:0:0:0:0:0:0:9000
```
After this the webserver has started and you can access the visualization UI by
directing your browser to [*http://localhost:10001/](http://localhost:10001/)

![screen](bidviz_interface.png)

You will see something like the above screen shot.

API Documentation
=================

When the webapp is first launched, it establishes a websocket connection to
/ws. All communication between the server and the javascript is done
through this websocket. Below describes different messages that the server
handles.

Client Initiated Message
------------------------

Client can send a message of this format:
```
{
    methodName: <methodName>;
    content: ...
}
```
The methodName refers the different methods that javascript wants to invoke
in the client. The different methodName the server accepts are:

* addFunction
* pauseTraining
* modifyParam
* evaluateCommand
* getCode
Below will describe what each of them do and what they accept as content.

### addFunction
Accepts
```
{
    methodName: "addFunction",
    content: {
        name: name,
        code: code,
        type: type
    }
}
```
Returns
```
{
    success: true,
    data: ""
}
```
if succeeds,
Or
```
{
    success: false,
    data: "error message"
}
```
if fails.

### pauseTraining

Accepts
```
{
    methodName: "pauseTraining",
    content: <boolean>
}
```
Content is true to pause and false to resume.
Returns: Nothing

### modifyParam
Accepts:
```
{
    methodName: "modifyParam",
    content: {
      // a map of key to newvalue
    }
}
```
The server will iterate over that map and set the value using reflexion.
Returns: Nothing



### evaluateCommand
Accepts:
```
{
    methodName: "evaluateCommand",
    content: {
        code: "// the command to evaluate"
    }
}
```
Execute to the command inside of "code".
Returns
```
{
    success: boolean,
    data: "string"
}
```
if success is true, then data is the result of the evaluation,
if success is false, then the data is the error message returned

### getCode
Accepts
```
{
    methodName: "evaluateCommand",
    content: {
       name : "name of the chart"
    }
}
```
get the Scala code user originally submitted to server for the chart of name.
Returns
```
{
    success: boolean,
    data: "scala code as string"
}
```

Server initialized messages
---------------------------

Server sends messages to client spontaneously on the following cases:
* on generating a new data point
* on run time message of the graph code
* sending requested parameters
