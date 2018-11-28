package uk.ac.wellcome.platform.sierra_reader.sink

import akka.Done
import akka.stream.scaladsl.Sink
import com.amazonaws.services.s3.AmazonS3
import io.circe.Json

import scala.concurrent.Future

object SequentialS3Sink {
  def apply(client: AmazonS3,
            bucketName: String,
            keyPrefix: String = "",
            offset: Int = 0): Sink[(Json, Long), Future[Done]] =
    Sink.foreach {
      case (json: Json, index: Long) => {
        // Zero-pad the index to four digits for easy sorting,
        // e.g. "1" ~> "0001", "25" ~> "0025"
        val key = f"$keyPrefix${index + offset}%04d.json"
        client.putObject(bucketName, key, json.noSpaces)
      }
    }
}
