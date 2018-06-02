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

trait StorageStrategy[T] {
  def store(t: T): StorageStream
  def retrieve(input: InputStream): Try[T]
}

object StorageStrategy {

  def apply[T](implicit strategy: StorageStrategy[T]): StorageStrategy[T] =
    strategy

  implicit def typeStorageStrategy[T](
    implicit jsonStorageStrategy: StorageStrategy[Json],
    encoder: Encoder[T],
    decoder: Decoder[T]
  ) = new StorageStrategy[T] {
    def store(t: T): StorageStream = jsonStorageStrategy.store(t.asJson)
    def retrieve(input: InputStream) =
      jsonStorageStrategy.retrieve(input).flatMap(_.as[T].toTry)

  }

  implicit def streamStorageStrategy: StorageStrategy[InputStream] =
    new StorageStrategy[InputStream] {
      def store(t: InputStream): StorageStream = {
        val s = fromInputStream(t).mkString

        val key = StorageKey(hash(s))
        val input = new ByteArrayInputStream(s.getBytes)

        StorageStream(input, key)
      }

      def retrieve(input: InputStream) = Success(input)
    }

  implicit def jsonStorageStrategy: StorageStrategy[Json] =
    new StorageStrategy[Json] {
      def store(t: Json): StorageStream = {
        val key = StorageKey(hash(t.noSpaces))
        val input = new ByteArrayInputStream(t.noSpaces.getBytes)

        StorageStream(input, key)
      }

      def retrieve(input: InputStream) =
        parse(Source.fromInputStream(input).mkString).toTry

    }

  implicit def stringStorageStrategy: StorageStrategy[String] =
    new StorageStrategy[String] {
      def store(t: String): StorageStream = {
        val key = StorageKey(hash(t))
        val input = new ByteArrayInputStream(t.getBytes)

        StorageStream(input, key)
      }

      def retrieve(input: InputStream) =
        Success(Source.fromInputStream(input).mkString)

    }

  private def hash(s: String) =
    MurmurHash3
      .stringHash(s, MurmurHash3.stringSeed)
      .toHexString

}
