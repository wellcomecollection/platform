package uk.ac.wellcome.messaging.utils

import io.circe.generic.AutoDerivation
import io.circe.java8.time.TimeInstances
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.scalatest.Matchers

import scala.util.Try

object JsonUtil extends AutoDerivation with TimeInstances with Matchers {

  private[messaging] def toJson[T](value: T)(implicit encoder: Encoder[T]): Try[String] = {
    assert(encoder != null)
    Try(value.asJson.noSpaces)
  }

  private[messaging] def toMap[T](json: String)(
    implicit decoder: Decoder[T]): Try[Map[String, T]] = {
    assert(decoder != null)
    fromJson[Map[String, T]](json)
  }

  private[messaging] def fromJson(json: String): Try[Json] =
    parse(json).toTry

  private[messaging] def fromJson[T](json: String)(implicit decoder: Decoder[T]): Try[T] = {
    assert(decoder != null)
    decode[T](json).toTry
  }

  private[messaging] def assertJsonStringsAreEqual(json1: String, json2: String) = {
    val tree1 = parse(json1).right.get
    val tree2 = parse(json2).right.get
    tree1 shouldBe tree2
  }

  private[messaging] def assertJsonStringsAreDifferent(json1: String, json2: String) = {
    val tree1 = parse(json1).right.get
    val tree2 = parse(json2).right.get
    tree1 shouldNot be(tree2)
  }
}
