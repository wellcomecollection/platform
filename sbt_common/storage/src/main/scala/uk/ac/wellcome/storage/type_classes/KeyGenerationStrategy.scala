package uk.ac.wellcome.storage.type_classes

import java.io.InputStream

import io.circe.Json

import scala.io.Source.fromInputStream
import scala.util.hashing.MurmurHash3

trait KeyGenerationStrategy[T] {
  def getKey(t: T): String
}

object KeyGenerator {

  private def hash(s: String) = MurmurHash3
    .stringHash(s, MurmurHash3.stringSeed)
    .toHexString

  def getKey[T](t: T)(implicit keyGetter: KeyGenerationStrategy[T]) =
    keyGetter.getKey(t)

  implicit val inputStreamKeyGetter: KeyGenerationStrategy[InputStream] =
    new KeyGenerationStrategy[InputStream] {
      def getKey(t: InputStream): String = hash(fromInputStream(t).mkString)
    }

  implicit val stringKeyGetter: KeyGenerationStrategy[String] =
    new KeyGenerationStrategy[String] {
      def getKey(t: String): String = hash(t)
    }

  implicit val jsonKeyGetter: KeyGenerationStrategy[Json] =
    new KeyGenerationStrategy[Json] {
      def getKey(t: Json): String = hash(t.noSpaces)
    }
}
