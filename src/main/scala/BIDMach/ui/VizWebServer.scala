package BIDMach.ui

import java.io.File

import akka.actor.{ActorSystem,ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.ws._//{ TextMessage, Message }
import akka.util.ByteString;
import akka.stream.scaladsl.{ Source, Flow,Sink }
import akka.http.scaladsl._
import java.util.logging.Logger;
import akka.stream._
import scala.collection.mutable.ListBuffer

import scala.concurrent.duration._    

/**
  * A simple lightweight web server built upon Akka http framework 
  * http://doc.akka.io/docs/akka-http/current/scala/http/index.html
  * Please override the routes before using it
  * The web sever will be running as soon as you instantiate the class.
  * See the examples below in object WebServer, supporting WebSocket and static file serving
  **/
    
class VizWebServer(val port:Int = 8888) {
    
    implicit val system = ActorSystem("BID-System")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tH:%1$tM:%1$tS %4$s: %5$s%n");
    val logger = Logger.getLogger("BID-WebServer console logging")
    
    // A more complex WebSocket handler 
    // See reference https://github.com/johanandren/chat-with-akka-http-websockets/blob/akka-2.4.9/src/main/scala/chat/Server.scala
    // Use Flow, Source and Sink http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0/scala/stream-flows-and-basics.html

    var sender : ActorRef = _
            
    def handler(): Flow[Message, Message, _] = {
      val incomingMessages: Sink[Message,_] =
          Sink.foreach[Message](x=>x match {
                          case TextMessage.Strict(text)=>{
                              println(text);
                          }
      })

      val outgoingMessages: Source[Message, _] =
        Source.actorRef[Any](10, OverflowStrategy.fail)
        .mapMaterializedValue { outActor =>
          // Save the actor so that we could send messages out
          sender = outActor
        }.map(
          // transform domain message to web socket message
          {
              case (outMsg: String) => TextMessage(outMsg)
              case (outMsg:Int) => TextMessage(outMsg.toString)
              case _=> TextMessage("Error")
          }
      )
      // then combine both to a flow
      Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
    }
    
    //Default route rules
    val route = 
      path("") {
        get {
            getFromResource("ui.html")
        }
      }~
      path(Remaining) { id=>
                            getFromResource(id)
                      }~
      path("ws") {
        get {
          handleWebSocketMessages(handler)
        }
      }
            
    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", port)
    logger.info("Server started at 0.0.0.0:" + port)
    
  def close() {
      bindingFuture
          .flatMap(_.unbind()) // trigger unbinding from the port
          .onComplete(_ => system.terminate()) // and shutdown when done
      logger.info("Server closed")
  }    
}   
    
object VizWebServer {
}
