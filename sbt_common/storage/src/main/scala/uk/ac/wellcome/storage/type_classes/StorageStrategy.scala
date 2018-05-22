package uk.ac.wellcome.storage.type_classes

import java.io.{ByteArrayInputStream, InputStream}

import io.circe.Json

import scala.io.Source.fromInputStream
import scala.util.hashing.MurmurHash3

case class StorageKey(val value: String) extends AnyVal
case class StorageStream(inputStream: InputStream, storageKey: StorageKey)

// This type class describes an implementation that takes a type T
// and produces a java.io.InputStream and a StorageKey indicating
// a unique storage path.

trait StorageStrategy[T] {
  def get(t: T): StorageStream
}

object StorageStrategyGenerator {

  private def hash(s: String) =
    MurmurHash3
      .stringHash(s, MurmurHash3.stringSeed)
      .toHexString

  implicit val inputStreamKeyGetter: StorageStrategy[InputStream] =
    new StorageStrategy[InputStream] {
      def get(t: InputStream): StorageStream = {
        val s = fromInputStream(t).mkString

        val key = StorageKey(hash(s))
        val input = new ByteArrayInputStream(s.getBytes)

        StorageStream(input, key)
      }
    }

  implicit val stringKeyGetter: StorageStrategy[String] =
    new StorageStrategy[String] {
      def get(t: String): StorageStream = {
        val key = StorageKey(hash(t))
        val input = new ByteArrayInputStream(t.getBytes)

        StorageStream(input, key)
      }
    }

  implicit val jsonKeyGetter: StorageStrategy[Json] =
    new StorageStrategy[Json] {
      def get(t: Json): StorageStream = {
        val key = StorageKey(hash(t.noSpaces))
        val input = new ByteArrayInputStream(t.noSpaces.getBytes)

        StorageStream(input, key)
      }
    }

}
