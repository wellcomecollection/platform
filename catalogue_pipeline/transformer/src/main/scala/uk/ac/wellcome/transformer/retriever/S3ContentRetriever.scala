package uk.ac.wellcome.transformer.retriever

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import uk.ac.wellcome.exceptions.GracefulFailureException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class S3ContentRetriever(s3Client: AmazonS3, bucketName: String) {
  def getS3Content(s3Key: String): Future[String] =
    Future {
      Source
        .fromInputStream(
          s3Client.getObject(bucketName, s3Key).getObjectContent)
        .mkString
    }.recover {
      case ex: AmazonS3Exception if ex.getErrorCode == "NoSuchKey" =>
        throw GracefulFailureException(ex)
    }
}
