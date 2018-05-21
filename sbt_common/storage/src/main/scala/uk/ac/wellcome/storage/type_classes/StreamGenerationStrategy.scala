package uk.ac.wellcome.storage.type_classes

import java.io.{ByteArrayInputStream, InputStream}

import io.circe.Json

trait StreamGenerationStrategy[T] {
  def getStream(t: T): InputStream
}

object StreamGenerator {

  def getStreamStrategy[T](t: T)(implicit streamGenerator: StreamGenerationStrategy[T]) =
    streamGenerator.getStream(t)

  implicit val inputStreamGenerator: StreamGenerationStrategy[InputStream] =
    new StreamGenerationStrategy[InputStream] {
      def getStream(t: InputStream) = t
    }

  implicit val stringGenerator: StreamGenerationStrategy[String] =
    new StreamGenerationStrategy[String] {
      def getStream(t: String) = new ByteArrayInputStream(t.getBytes)
    }

  implicit val typeGenerator: StreamGenerationStrategy[Json] =
    new StreamGenerationStrategy[Json] {
      def getStream(t: Json) = new ByteArrayInputStream(t.noSpaces.getBytes)
    }

}