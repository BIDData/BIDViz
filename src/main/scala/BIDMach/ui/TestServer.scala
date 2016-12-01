package BIDMach.ui
import java.io.File
import play.core.server.NettyServer
import play.api.routing.sird.GET
import play.api.mvc.Action
import play.api.mvc.Results
import play.api.mvc.WebSocket

import play.api.ApplicationLoader.Context
import play.api._
import play.api.routing.Router
import play.api.routing._
import play.api.routing.sird._
import play.api.mvc._
import play.core.server.{ServerConfig, NettyServer}
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

class TestServer {
  var channel_ = null
  var func: (String)=>Unit = null

  def startServer() = {
    val server = NettyServer.fromRouter() {
      case GET(p"/") => Action {
        // var staticFile = new File("./public/index.html")
        Results.Ok("hello")
      }

      case GET(p"/ws") => {
        println("here")
        //WebSocket Example, more details here: https://www.playframework.com/documentation/2.5.x/ScalaWebSockets
        WebSocket.using[String] {
          println("here2")
          request =>
            // Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
            val (out, channel) = Concurrent.broadcast[String]
            println(channel.getClass)

            // log the message to stdout and send response back to client
            val in = Iteratee.foreach[String] {
              msg =>
                println(channel.getClass)
                println(msg)
                // the Enumerator returned by Concurrent.broadcast subscribes to the channel and will
                // receive the pushed messages
                for (i <- 1 to 100)
                  channel push ("I received your message: " + i)
                func = channel.push _
            }
            (in, out)
        }
      }
    }
  }
}


