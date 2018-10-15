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

  val pendingString = "pending"
  val succeededString = "succeeded"
  val failedString = "failed"

  case object Pending extends Status {
    override def toString: String = pendingString
  }

  case object Succeeded extends Status {
    override def toString: String = succeededString
  }

  case object Failed extends Status {
    override def toString: String = failedString
  }

  def parseStatus(string: String): Status = {
    string match {
      case `pendingString`   => Pending
      case `succeededString` => Succeeded
      case `failedString`    => Failed
    }
  }
}

trait CallbackStatusConverters {
  import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}
  import Callback._

  implicit val encoder: Encoder[Status] = Encoder.instance[Callback.Status] {
    status: Callback.Status =>
      Json.fromString(status.toString)
  }

  implicit val decoder: Decoder[Status] =
    Decoder.instance[Callback.Status](cursor =>
      for {
        status <- cursor.value.as[String]
      } yield {
        parseStatus(status)
    })

  implicit val fmtStatus: AnyRef with DynamoFormat[Status] =
    DynamoFormat.xmap[Callback.Status, String](
      fromJson[Callback.Status](_)(decoder).toEither.left
        .map(TypeCoercionError)
    )(
      toJson[Callback.Status](_).get
    )
}
