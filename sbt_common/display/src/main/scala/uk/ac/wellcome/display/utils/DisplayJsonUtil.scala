package uk.ac.wellcome.display.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scala.util.Try

// We use Circe for all of our internal serialisation in the Platform, but
// for displaying works in the API (and by extension, anywhere else we produce
// JSON serialisations of display models), we use Jackson.
//
// This is because Jackson is very closely tied to Finatra.
//
object DisplayJsonUtil {

  val mapper = new ObjectMapper() with ScalaObjectMapper

  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  // This serialisation option means that if the value would be an empty list
  // or null, we don't include it in the JSON body.  This is a desirable
  // feature in the API outputs.
  mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)

  def toJson(value: Any): Try[String] =
    Try(mapper.writeValueAsString(value))

  def fromJson[T](json: String)(implicit m: Manifest[T]): Try[T] =
    Try(mapper.readValue[T](json))
}
