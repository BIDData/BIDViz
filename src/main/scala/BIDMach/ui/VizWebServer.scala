package BIDMach.ui

import java.io.File

import play.api.ApplicationLoader.Context
import play.api._
import play.api.routing.Router
import play.api.routing._
import play.api.routing.sird._
import play.api.mvc._
import play.core.server.{NettyServer, ServerConfig}
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

/**
  * Created by han on 2/20/17.
  */
class VizWebServer(webServerChannel: WebServerChannel) {

  var func: Function[String, Unit] = null
  val environment = new Environment(
    new File("."),
    getClass.getClassLoader,
    play.api.Mode.Dev
  )
  val context = ApplicationLoader.createContext(environment)

  def routes:Router.Routes = {
    case GET(p"/") => controllers.Assets.at(path="/public/dataflow_viz", file="index.html")
    case GET(p"/ws")=>      //WebSocket Example, more details here: https://www.playframework.com/documentation/2.5.x/ScalaWebSockets
      WebSocket.using[String] {
        request =>
          // Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
          val (out, channel) = Concurrent.broadcast[String]

          val in = Iteratee.foreach[String] {
            msg =>
              func = channel push _
              try {
                val jsonVal = Json.parse(msg)
                val requestId = (jsonVal \ "id").as[String]
                val content = (jsonVal \ "content").as[JsValue]
                var message = webServerChannel.handleRequest(content)
                message = CallbackMessage(requestId, message.success, message.data)
                println("HAN", message)
                // the Enumerator returned by Concurrent.broadcast subscribes to the channel and will
                // receive the pushed messages
                channel push Json.toJson(Seq(Message("callback", message))).toString()
              } catch {
                case x: Exception =>
                  x.printStackTrace()
              }
          }
          (in,out)
      }
    case GET(p"/assets/$file*")=>
      controllers.Assets.at(path="/public", file=file)
    case GET(p"/$file*")=>
      controllers.Assets.at(path="/public/dataflow_viz", file=file)
  }

  val components = new BuiltInComponentsFromContext(context) {
    override def router: Router = Router.from(routes)
  }

  val applicationLoader = new ApplicationLoader {
    override def load(context: Context): Application = components.application
  }

  val application = applicationLoader.load(context)

  Play.start(application)

  private object CausedBy {
    def unapply(e: Throwable): Option[Throwable] = Option(e.getCause)
  }

  private def startFindPort(port:Int): (Int, NettyServer) = {
    try {
      (port, NettyServer.fromApplication(application, ServerConfig(port = Some(port))))
    } catch {
      case CausedBy(e : java.net.BindException) => {
        startFindPort(port + 1)
      }
    }
  }

  def startPort = 10001
  val (port, server) = startFindPort(startPort)
  def stop() = server.stop()
}

