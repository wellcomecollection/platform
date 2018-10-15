package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.net.URI

import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.TypeCoercionError
import io.circe.{Decoder, Encoder, Json}
import uk.ac.wellcome.platform.archive.common.json.URIConverters

case class Callback(callbackUri: URI, callbackStatus: Callback.Status)

case object Callback extends URIConverters with CallbackStatusConverters {
  sealed trait Status

  def apply(callbackUri: URI): Callback = {
    Callback(callbackUri = callbackUri, callbackStatus = Pending)
  }

  val PendingString = "pending"
  val SucceededString = "succeeded"
  val FailedString = "failed"

  case object Pending extends Status {
    override def toString: String = PendingString
  }

  case object Succeeded extends Status {
    override def toString: String = SucceededString
  }

  case object Failed extends Status {
    override def toString: String = FailedString
  }

  def parseStatus(string: String): Status = {
    string match {
      case PendingString   => Pending
      case SucceededString => Succeeded
      case FailedString    => Failed
    }
  }
}

trait CallbackStatusConverters {
  import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}
  import Callback._

  implicit val enc: Encoder[Status] = Encoder.instance[Callback.Status] {
    status: Callback.Status =>
      Json.fromString(status.toString)
  }

  implicit val dec: Decoder[Status] =
    Decoder.instance[Callback.Status](cursor =>
      for {
        status <- cursor.value.as[String]
      } yield {
        parseStatus(status)
    })

  implicit val fmtStatus: AnyRef with DynamoFormat[Status] =
    DynamoFormat.xmap[Callback.Status, String](
      fromJson[Callback.Status](_)(dec).toEither.left
        .map(TypeCoercionError)
    )(
      toJson[Callback.Status](_).get
    )
}
