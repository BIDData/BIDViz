package BIDMach.ui


import BIDMach.models.Model
import BIDMat.Mat
import play.api.libs.json.{Json, Writes}

/**
  * Created by han on 1/19/17.
  */
trait MessageContent {}
case class Message(msgType: String, content: MessageContent){}


case class DataPointContent(name: String, ipass: Int,
                            shape: Seq[Int], data: Seq[String]) extends MessageContent {}

case class ParameterContent(content: Map[String, String]) extends MessageContent {}

case class StatFunction(name: String, code: String, size: Int,
                        theType: String, funcPointer: (Model, Array[Mat]) => Mat) extends MessageContent{}

object Message {
  implicit val contentWrite = new Writes[MessageContent] {
    def writes(content: MessageContent) = content match {
      case DataPointContent(name, ipass, shape, data) =>
        Json.obj(
          "name" -> name,
        "ipass" -> ipass,
        "shape" -> shape,
        "data" -> data
      )
      case ParameterContent(c) => Json.toJson(c)
    }
  }

  implicit  val messageWrite = new Writes[Message]  {
    def writes(content: Message) = Json.obj(
      "msgType"-> content.msgType,
      "content"-> Json.toJson(content.content)
    )
  }

}