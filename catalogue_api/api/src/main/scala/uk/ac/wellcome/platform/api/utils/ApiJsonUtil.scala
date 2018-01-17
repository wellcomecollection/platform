package uk.ac.wellcome.platform.api.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scala.util.Try

object ApiJsonUtil {
  val mapper = new ObjectMapper() with ScalaObjectMapper

  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  // This serialisation option means that if the value would be an empty list
  // or null, we don't include it in the JSON body.  This is a desirable
  // feature in the API outputs.
  mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)

  def fromJson[T](json: String)(implicit m: Manifest[T]): Try[T] =
    Try(mapper.readValue[T](json))
}
