package uk.ac.wellcome.storage.type_classes

import java.io.{ByteArrayInputStream, InputStream}

import io.circe.Json

import scala.io.Source.fromInputStream
import scala.util.hashing.MurmurHash3

trait StorageStrategy[T] {
  def get(t: T): (InputStream, String)
}

object StorageStrategyGenerator {

  private def hash(s: String) = MurmurHash3
    .stringHash(s, MurmurHash3.stringSeed)
    .toHexString

  def getKey[T](t: T)(implicit strategy: StorageStrategy[T]) =
    strategy.get(t)

  implicit val inputStreamKeyGetter: StorageStrategy[InputStream] =
    new StorageStrategy[InputStream] {
      def get(t: InputStream): (InputStream, String) = {
        val s = fromInputStream(t).mkString

        val key = hash(s)
        val input = new ByteArrayInputStream(s.getBytes)

        (input, key)
      }
    }

  implicit val stringKeyGetter: StorageStrategy[String] =
    new StorageStrategy[String] {
      def get(t: String): (InputStream, String) = {
        val key = hash(t)
        val input = new ByteArrayInputStream(t.getBytes)

        (input, key)
      }
    }

  implicit val jsonKeyGetter: StorageStrategy[Json] =
    new StorageStrategy[Json] {
      def get(t: Json): (InputStream, String) = {
        val key = hash(t.noSpaces)
        val input = new ByteArrayInputStream(t.noSpaces.getBytes)

        (input, key)
      }
    }

}
