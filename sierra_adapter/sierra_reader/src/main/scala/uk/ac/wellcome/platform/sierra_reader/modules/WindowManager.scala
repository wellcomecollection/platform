package uk.ac.wellcome.platform.sierra_reader.modules

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import org.apache.commons.io.IOUtils
import uk.ac.wellcome.platform.sierra_reader.models.{SierraConfig, WindowStatus}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber
import uk.ac.wellcome.sierra_adapter.models.SierraRecord
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.utils.JsonUtil

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
        val triedMaybeLastId: Try[Option[String]] =
          JsonUtil.fromJson[List[SierraRecord]](lastBody).map { r =>
            r.map { _.id.withoutCheckDigit }.sorted.lastOption
          }

        info(s"Found latest ID in S3: $triedMaybeLastId")
        val triedStatus = triedMaybeLastId
          .map {
            case Some(id) =>
              WindowStatus(
                id = Some(incrementSierraRecordNumber(id)),
                offset = offset + 1
              )
            case None =>
              throw GracefulFailureException(
                new RuntimeException("Json did not contain an id"))
          }

        // Let it throw the exception if it's a failure
        triedStatus.get
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

  def incrementSierraRecordNumber(id: String): SierraRecordNumber =
    SierraRecordNumber(
      (id.toInt + 1).toString
    )

}
