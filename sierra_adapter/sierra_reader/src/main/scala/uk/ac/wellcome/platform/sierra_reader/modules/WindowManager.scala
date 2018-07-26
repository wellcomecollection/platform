package uk.ac.wellcome.platform.sierra_reader.modules

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.parser.parse
import io.circe.optics.JsonPath.root
import org.apache.commons.io.IOUtils
import uk.ac.wellcome.platform.sierra_reader.models.SierraConfig
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.storage.s3.S3Config

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

case class WindowStatus(id: Option[String], offset: Int)

class WindowManager @Inject()(
  s3client: AmazonS3,
  s3Config: S3Config,
  sierraConfig: SierraConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def getCurrentStatus(window: String): Future[WindowStatus] = Future {
    info(
      s"Searching for existing records in prefix ${buildWindowShard(window)}")

    val lastExistingKey = s3client
      .listObjects(s3Config.bucketName, buildWindowShard(window))
      .getObjectSummaries
      .asScala
      .map { _.getKey() }
      .sorted
      .lastOption

    info(s"Found latest JSON file in S3: $lastExistingKey")

    lastExistingKey match {
      case Some(key) => {

        // Our SequentialS3Sink creates filenames that end 0000.json, 0001.json, ..., with an optional prefix.
        // Find the number on the end of the last file.
        val embeddedIndexMatch = "(\\d{4})\\.json$".r.unanchored
        val offset = key match {
          case embeddedIndexMatch(index) => index.toInt
          case _ =>
            throw GracefulFailureException(
              new RuntimeException(s"Unable to determine offset in $key"))
        }

        val lastBody = IOUtils.toString(
          s3client.getObject(s3Config.bucketName, key).getObjectContent)

        val lastId = getId(lastBody)

        info(s"Found latest ID in S3: $lastId")

        // The Sierra IDs we store in S3 are prefixed with "b" or "i".
        // Remove the first character
        val unprefixedId = lastId.substring(1)

        val newId = (unprefixedId.toInt + 1).toString
        WindowStatus(id = Some(newId), offset = offset + 1)
      }
      case None => WindowStatus(id = None, offset = 0)
    }
  }

  def buildWindowShard(window: String) =
    s"records_${sierraConfig.resourceType.toString}/${buildWindowLabel(window)}/"

  def buildWindowLabel(window: String) =
    // Window is a string like [2013-12-01T01:01:01Z,2013-12-01T01:01:01Z].
    // We discard the square braces, colons and comma so we get slightly nicer filenames.
    window
      .replaceAll("\\[", "")
      .replaceAll("\\]", "")
      .replaceAll(":", "-")
      .replaceAll(",", "__")

  private def getId(jsonString: String): String =
    parse(jsonString) match {
      case Right(json) => root.id.string.getOption(json).get
      case Left(_) => throw GracefulFailureException(
        new RuntimeException(s"JSON <<$jsonString>> didn't contain an id"))
    }
}
