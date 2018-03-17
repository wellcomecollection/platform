package uk.ac.wellcome.platform.snapshot_convertor.services

import com.amazonaws.services.s3.model.ObjectMetadata
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.snapshot_convertor.models.{CompletedConversionJob, ConversionJob}
import uk.ac.wellcome.test.fixtures.{S3, TestWith}
import uk.ac.wellcome.utils.JsonUtil

import scala.io.Source

class ConvertorServiceTest
  extends FunSpec
    with ScalaFutures
    with Matchers
    with S3 {

  def withConvertorService[R](testWith: TestWith[ConvertorService, R]) = {
    new ConvertorService(s3Client = s3Client)
  }

  it("converts a gzipped elasticdump from S3 into the correct format in the target bucket") {
    withLocalS3Bucket { bucketName =>

      val key = "elastic_dump_example.txt.gz"
      val input = getClass.getResourceAsStream("/elastic_dump_example.txt.gz")
      val metadata = new ObjectMetadata()

      s3Client.putObject(bucketName, key, input, metadata)

      withConvertorService { service =>

        val conversionJob = ConversionJob(
          bucketName = bucketName,
          objectKey = key
        )

        val future = service.runConversion(conversionJob)

        whenReady(future) { completedConversionJob: CompletedConversionJob =>

          val displayWorks = Source.fromURL(completedConversionJob.targetLocation)
            .mkString.split("\n")
          
          false shouldBe true
        }
      }
    }
  }
}
