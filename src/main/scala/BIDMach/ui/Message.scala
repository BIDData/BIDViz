package BIDMach.ui


import BIDMach.Learner
import BIDMach.models.Model
import BIDMat.Mat
import play.api.libs.json.{Json, Writes}
import scala.collection.mutable.{Map => MMap}

/**
  * Created by han on 1/19/17.
  */
trait MessageContent {}
case class Message(msgType: String, content: MessageContent){}

case class DataPointContent(name: String, ipass: Int,
                            shape: Seq[Int], data: Seq[String], theType: String) extends MessageContent {}

case class ParameterContent(content: Map[String, String]) extends MessageContent {}

case class StatFunction(name: String, code: String, size: Int,
                        theType: String, funcPointer: (Model, Array[Mat], Learner) => Mat,
                        uiDefinition: String) extends MessageContent{}

case class CallbackMessage(id: String, success: Boolean, data: String) extends MessageContent {}
case class ErrorMessage(msg: String) extends MessageContent {}


object Message {
  implicit val contentWrite = new Writes[MessageContent] {
    def writes(content: MessageContent) = content match {
      case DataPointContent(name, ipass, shape, data, theType) =>
        Json.obj(
          "name" -> name,
          "ipass" -> ipass,
          "shape" -> shape,
          "data" -> data,
          "type" -> theType
        )
      case ParameterContent(c) => Json.toJson(c)
      case ErrorMessage(msg) => Json.obj(
        "msg" -> msg
      )
      case CallbackMessage(id, success, data) => Json.obj(
        "id" -> id,
        "success" -> success,
        "data" -> data
      )
      case StatFunction(name, code, size, theType, func, ui) => Json.obj(
        "name" -> name,
        "code" -> code,
        "size" -> size,
        "type" -> theType,
        "ui" -> ui
      )
    }
  }

  implicit  val messageWrite = new Writes[Message]  {
    def writes(content: Message) = Json.obj(
      "msgType"-> content.msgType,
      "content"-> Json.toJson(content.content)
    )
  }

}