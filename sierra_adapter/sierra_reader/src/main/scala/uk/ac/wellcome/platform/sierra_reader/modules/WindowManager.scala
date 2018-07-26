package uk.ac.wellcome.platform.sierra_reader.modules

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import org.apache.commons.io.IOUtils
import uk.ac.wellcome.platform.sierra_reader.models.SierraConfig
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.utils.JsonUtil._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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

        val maybeLastId = getLastId(lastBody)

        info(s"Found latest ID in S3: $maybeLastId")

        maybeLastId match {
          case Some(id) =>
            // The Sierra IDs we store in S3 are prefixed with "b" or "i".
            // Remove the first character
            val unprefixedId = id.substring(1)

            val newId = (unprefixedId.toInt + 1).toString
            WindowStatus(id = Some(newId), offset = offset + 1)
          case None =>
            throw GracefulFailureException(
              new RuntimeException(s"JSON <<$lastBody>> did not contain an id"))
        }
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

  // The contents of our S3 files should be an array of either SierraBibRecord
  // or SierraItemRecord; we want to get the last ID of the current contents
  // so we know what to ask the Sierra API for next.
  //
  private def getLastId(s3contents: String): Option[String] = {
    case class Identified(id: String)

    fromJson[List[Identified]](s3contents) match {
      case Success(ids) => ids.map { _.id }.sorted.lastOption
      case Failure(_) =>
        throw GracefulFailureException(
          new RuntimeException(
            s"S3 contents <<$s3contents> could not be parsed as JSON"))
    }
  }
}
