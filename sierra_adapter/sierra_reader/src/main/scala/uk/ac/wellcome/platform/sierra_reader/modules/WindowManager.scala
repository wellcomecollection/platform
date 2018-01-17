package uk.ac.wellcome.platform.sierra_reader.modules

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import org.apache.commons.io.IOUtils
import io.circe.generic.auto._
import io.circe.parser.decode
import uk.ac.wellcome.platform.sierra_reader.flow.SierraResourceTypes
import uk.ac.wellcome.circe._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.collection.JavaConversions._
import scala.concurrent.Future

case class WindowStatus(id: Option[String], offset: Int)

class WindowManager @Inject()(
  s3client: AmazonS3,
  @Flag("aws.s3.bucketName") bucketName: String,
  @Flag("sierra.fields") fields: String,
  resourceType: SierraResourceTypes.Value
) extends Logging {

  def getCurrentStatus(window: String): Future[WindowStatus] = Future {
    info(
      s"Searching for existing records in prefix ${buildWindowShard(window)}")

    val lastExistingKey = s3client
      .listObjects(bucketName, buildWindowShard(window))
      .getObjectSummaries
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
          s3client.getObject(bucketName, key).getObjectContent)
        val records = decode[List[SierraRecord]](lastBody)
        val lastId = records match {
          case Right(r) =>
            r.map { _.id }.sorted.lastOption
          case Left(err) => throw GracefulFailureException(err)
        }

        info(s"Found latest ID in S3: $lastId")
        lastId
          .map(id => {
            // The Sierra IDs we store in S3 are prefixed with "b" or "i".
            // Remove the first character
            val unprefixedId = id.substring(1)

            val newId = (unprefixedId.toInt + 1).toString
            WindowStatus(id = Some(newId), offset = offset + 1)
          })
          .getOrElse(
            throw GracefulFailureException(
              new RuntimeException("Json did not contain an id"))
          )
      }
      case None => WindowStatus(id = None, offset = 0)
    }
  }

  def buildWindowShard(window: String) =
    s"records_${resourceType.toString}/${buildWindowLabel(window)}/"

  def buildWindowLabel(window: String) =
    // Window is a string like [2013-12-01T01:01:01Z,2013-12-01T01:01:01Z].
    // We discard the square braces, colons and comma so we get slightly nicer filenames.
    window
      .replaceAll("\\[", "")
      .replaceAll("\\]", "")
      .replaceAll(":", "-")
      .replaceAll(",", "__")

}
