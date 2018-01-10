package uk.ac.wellcome.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.parser._
import cats.syntax.either._
import io.circe.Printer

import scala.util.Try

object JsonUtil {
  val mapper = new ObjectMapper() with ScalaObjectMapper

  val printer = Printer.noSpaces.copy(dropNullValues = true)

  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JavaTimeModule())
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  // This serialisation option means that if the value would be an empty list
  // or null, we don't include it in the JSON body.  This is a desirable
  // feature in the API outputs.
  mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
  def toJsonCirce[T](value: T)(implicit encoder: Encoder[T]): Try[String] = {
    import io.circe.generic.auto._
    import uk.ac.wellcome.circe._
    import uk.ac.wellcome.models.IdentifierSchemes._
    Try(printer.pretty(value.asJson))
  }

  def toJson(value: Any): Try[String] =
    Try(mapper.writeValueAsString(value))

  def fromJsonCirce[T](json:String)(implicit decoder: Decoder[T]): Try[T] = {
    decode[T](json).toTry
  }

  def toMap[V](json: String)(implicit m: Manifest[V]) =
    fromJson[Map[String, V]](json)

  def fromJson[T](json: String)(implicit m: Manifest[T]): Try[T] =
    Try(mapper.readValue[T](json))
}
