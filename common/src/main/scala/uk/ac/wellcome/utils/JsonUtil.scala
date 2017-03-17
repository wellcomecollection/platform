package uk.ac.wellcome.utils

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.util.Try


object JsonUtil {
  val mapper = new ObjectMapper() with ScalaObjectMapper

  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def toJson(value: Any): Try[String] =
    Try(mapper.writeValueAsString(value))

  def toMap[V](json:String)(implicit m: Manifest[V]) =
    fromJson[Map[String,V]](json)

  def fromJson[T](json: String)(implicit m : Manifest[T]): Try[T] =
    Try(mapper.readValue[T](json))
}
