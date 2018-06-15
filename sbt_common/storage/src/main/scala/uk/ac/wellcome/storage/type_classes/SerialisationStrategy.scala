package uk.ac.wellcome.storage.type_classes

import java.io.{ByteArrayInputStream, InputStream}

import io.circe.{Decoder, Encoder, Json}

import scala.io.Source
import scala.io.Source.fromInputStream
import io.circe.parser._
import io.circe.syntax._

import scala.util.{Success, Try}
import scala.util.hashing.MurmurHash3

case class StorageKey(val value: String) extends AnyVal
case class StorageStream(inputStream: InputStream, storageKey: StorageKey)

// This type class describes an implementation that takes a type T
// and produces a java.io.InputStream and a StorageKey indicating
// a unique storage path.

trait SerialisationStrategy[T] {
  def toStream(t: T): StorageStream
  def fromStream(input: InputStream): Try[T]
}

object SerialisationStrategy {

  def apply[T](
    implicit strategy: SerialisationStrategy[T]): SerialisationStrategy[T] =
    strategy

  implicit def typeStorageStrategy[T](
    implicit jsonStorageStrategy: SerialisationStrategy[Json],
    encoder: Encoder[T],
    decoder: Decoder[T]
  ) = new SerialisationStrategy[T] {
    def toStream(t: T): StorageStream = jsonStorageStrategy.toStream(t.asJson)
    def fromStream(input: InputStream) =
      jsonStorageStrategy.fromStream(input).flatMap(_.as[T].toTry)

  }

  implicit def streamStorageStrategy: SerialisationStrategy[InputStream] =
    new SerialisationStrategy[InputStream] {
      def toStream(t: InputStream): StorageStream = {
        val s = fromInputStream(t).mkString

        val key = StorageKey(hash(s))
        val input = new ByteArrayInputStream(s.getBytes)

        StorageStream(input, key)
      }

      def fromStream(input: InputStream) = Success(input)
    }

  implicit def jsonStorageStrategy: SerialisationStrategy[Json] =
    new SerialisationStrategy[Json] {
      def toStream(t: Json): StorageStream = {
        val key = StorageKey(hash(t.noSpaces))
        val input = new ByteArrayInputStream(t.noSpaces.getBytes)

        StorageStream(input, key)
      }

      def fromStream(input: InputStream) =
        parse(Source.fromInputStream(input).mkString).toTry

    }

  implicit def stringStorageStrategy: SerialisationStrategy[String] =
    new SerialisationStrategy[String] {
      def toStream(t: String): StorageStream = {
        val key = StorageKey(hash(t))
        val input = new ByteArrayInputStream(t.getBytes)

        StorageStream(input, key)
      }

      def fromStream(input: InputStream) =
        Success(Source.fromInputStream(input).mkString)

    }

  private def hash(s: String) =
    MurmurHash3
      .stringHash(s, MurmurHash3.stringSeed)
      .toHexString

}
