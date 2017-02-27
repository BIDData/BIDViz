System Design
=============


## 1. Goal

BIDMachViz targets the current user of BIDMach machine learning
librarying, enhancing and extending BIDMach library
with the capability to do real time visualization
and debugging. More specifically, we want to add
the following user stories:


1. As a scientist, I want to view real time statistics on 
   my current model while it is training and evolving.
2. As a scientist, I want to specify what kind of statistics 
    is relevant to my model through some scripting language, 
    and see the new statistic show up in my real time view. 
3. As a scientist, I want to be able to modify current hyperparameters 
   associated with my model, without restarting the current training process.
4. (stretch goal) As a scientist, I would like to define and package my custom
   visualization so I can use them for later project or share with the greater
   data science community.

Besides the above stories, we could like to design a solution that is easily
extensible by both the developers and users of BIDMach.

We want add the above instrumentation without impacting the running performance
models, and it should require as little modification to existing BIDMach scripts
to use as possible. 

## 2. Current system design

The system consists of the following parts:

1. BIDMach, the machine learning library
2. A web server that have access to the inner parameters inside BIDMach.
3. The Web UI responsible of displaying the data and interactions.

BIDMach is ... (description of BIDMach)

To make the webserver (VIZ) accessible to the data stored in BIDMach, we make
BIDMach's Learner object an "observable" object, and we attach a reference 
of a "listener" to every updates of the data. In other words, 
we define the following interface:

```
trait Observer {
    def notify(model:Model)
}
```

An observer will get notified on each X iterations of the training loop, 
and it will get a reference to the model object, which contains the current
parameter. In the case of the webserver, it will  computes a list of statistics on the model 
parameters, and then send them out to the UI, which manages and displays 
several charts and is responsible for routing the data to the corresponding 
charts.

The webserver is also responsible of creating new metrics to be computed. When it
receives a request to add a new statistics to be computed, in forms of a snippet of
Scala code. The webserver will compile that snippet into bytecode, then load it
into the current running environment using Java classloader. 

## Architecture of the Web Server

## Architecture of the Web UI

Web UI is built using jQuery



