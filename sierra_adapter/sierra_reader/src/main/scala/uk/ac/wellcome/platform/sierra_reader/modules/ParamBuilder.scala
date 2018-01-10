package uk.ac.wellcome.platform.sierra_reader.modules

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import io.circe
import org.apache.commons.io.IOUtils
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser.decode
import uk.ac.wellcome.platform.sierra_reader.flow.{SierraRecord, SierraResourceTypes}
import uk.ac.wellcome.circe._
import uk.ac.wellcome.sqs.SQSReaderGracefulException
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.collection.JavaConversions._
import scala.concurrent.Future

class ParamBuilder @Inject()(
  s3client: AmazonS3,
  @Flag("aws.s3.bucketName") bucketName: String,
  @Flag("sierra.fields") fields: String,
  @Flag("reader.resourceType") resourceType: SierraResourceTypes.Value
) extends Logging {

  def buildParams(window: String): Future[Map[String, String]] = Future {

    info(s"Searching for existing records in prefix ${buildWindowShard(window)}")

    val lastExistingKey = s3client
      .listObjects(bucketName, buildWindowShard(window))
      .getObjectSummaries
      .map { _.getKey() }
      .sorted
      .lastOption

    info(s"Found latest JSON file in S3: $lastExistingKey")

    val baseParams = Map("updatedDate" -> window, "fields" -> fields)

    lastExistingKey match {
      case Some(key) => {
        val lastBody = IOUtils.toString(s3client.getObject(bucketName, key).getObjectContent)
        val records = decode[List[SierraRecord]](lastBody)
        val lastId = records match {
          case Right(r) => r
              .map {_.id}
              .sorted
              .lastOption
          case Left(err) => throw SQSReaderGracefulException(err)
        }
        info(s"Found latest ID in S3: $lastId")
        lastId.map(id => baseParams ++ Map("id" -> (id.toInt + 1).toString))
          .getOrElse(
            throw SQSReaderGracefulException(new RuntimeException("Json did not contain an id"))
          )
      }
      case None => baseParams
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
