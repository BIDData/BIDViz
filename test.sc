/**
  * Created by zouxuan on 12/1/16.
  */

//#!/usr/bin/env scala
import BIDMach.ui.WebServerChannel

println("Hello, world!")
val channel = new WebServerChannel
var count = 0
while (true) {
  if (channel.server.func != null) {
    val y = Math.random() * 20 + 50
    channel.server.func("{\"x\": " + count + ", \"y\": " + y + "}")
    count += 1
    Thread.sleep(500)
  }
}


