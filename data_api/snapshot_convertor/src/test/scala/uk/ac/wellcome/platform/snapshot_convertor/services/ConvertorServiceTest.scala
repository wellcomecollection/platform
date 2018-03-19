package uk.ac.wellcome.platform.snapshot_convertor.services

import akka.http.scaladsl.model.Uri
import com.amazonaws.services.s3.model.ObjectMetadata
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.snapshot_convertor.models.{
  CompletedConversionJob,
  ConversionJob
}
import uk.ac.wellcome.test.fixtures.{S3, TestWith}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.models.{IdentifiedWork, WorksIncludes}

import scala.io.Source
import io.circe._
import io.circe.parser._

import scala.util.Try

class ConvertorServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with S3 {

  def withConvertorService[R](bucketname: String)(
    testWith: TestWith[ConvertorService, R]): R = {
    val convertorService = new ConvertorService(bucketname, s3Client)

    testWith(convertorService)
  }

  def withUncompressedTestDump[R](bucketName: String)(
    testWith: TestWith[(List[DisplayWork], String), R]): R = {
    val key = "elasticdump_example.txt"

    val expectedIdentifiedWorksGetResponseStream =
      getClass.getResourceAsStream(s"/$key")

    val expectedIdentifiedWorksGetResponseStrings =
      Source
        .fromInputStream(expectedIdentifiedWorksGetResponseStream)
        .mkString
        .split("\n")

    val expectedIdentifiedWorks =
      expectedIdentifiedWorksGetResponseStrings.map(getResponseString => {
        val source: Json =
          (parse(getResponseString).right.get \\ "_source").head
        source.as[IdentifiedWork].right.get
      })

    val includes = WorksIncludes(
      identifiers = true,
      thumbnail = true,
      items = true
    )

    val expectedDisplayWorks = expectedIdentifiedWorks.map(
      work =>
        DisplayWork(
          work = work,
          includes = includes
      ))

    val input = getClass.getResourceAsStream(s"/$key")
    val metadata = new ObjectMetadata()

    s3Client.putObject(bucketName, key, input, metadata)

    testWith((expectedDisplayWorks.toList, key))
  }

  it(
    "converts a gzipped elasticdump from S3 into the correct format in the target bucket") {
    withLocalS3Bucket { bucketName =>
      withUncompressedTestDump(bucketName) {
        case (expectedDisplayWorks, key) =>
          withConvertorService(bucketName) { service =>
            val conversionJob = ConversionJob(
              bucketName = bucketName,
              objectKey = key
            )

            val future = service.runConversion(conversionJob)

            whenReady(future) {
              completedConversionJob: CompletedConversionJob =>
                val inputStream = s3Client
                  .getObject(bucketName, "target.txt")
                  .getObjectContent()

                val displayWorksStrings =
                  Source.fromInputStream(inputStream).mkString.split("\n")

                val displayWorks: Array[DisplayWork] =
                  displayWorksStrings.map((displayWork) => {
                    println(displayWork)

                    JsonUtil.fromJson[DisplayWork](displayWork).get
                  })

                displayWorks should contain theSameElementsAs (expectedDisplayWorks)
            }
          }
      }
    }
  }
}
