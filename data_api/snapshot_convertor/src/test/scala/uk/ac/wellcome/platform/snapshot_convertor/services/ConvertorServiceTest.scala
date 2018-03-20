package uk.ac.wellcome.platform.snapshot_convertor.services

import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

import akka.http.scaladsl.model.Uri
import com.amazonaws.services.s3.model.ObjectMetadata
import com.twitter.finatra.json.FinatraObjectMapper
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.Matchers.all
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.snapshot_convertor.models.{CompletedConversionJob, ConversionJob}
import uk.ac.wellcome.test.fixtures.{AkkaFixtures, S3, TestWith}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.models.{IdentifiedWork, WorksIncludes}

import scala.io.Source
import io.circe._
import io.circe.parser._
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.ConvertorServiceFixture
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try

class ConvertorServiceTest
  extends FunSpec
    with ScalaFutures
    with AkkaFixtures
    with Matchers
    with ExtendedPatience
    with ConvertorServiceFixture {

  def withCompressedTestDump[R](bucketName: String)(
    testWith: TestWith[(List[DisplayWork], String), R]): R = {

    val key = "elasticdump_example.txt.gz"

    val expectedIdentifiedWorksGetResponseStream =
      new GZIPInputStream(new BufferedInputStream(getClass.getResourceAsStream(s"/$key")))

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

    withConvertorService { fixtures =>
      withCompressedTestDump(fixtures.bucketName) { case (expectedDisplayWorks, key) =>

          val conversionJob = ConversionJob(
            bucketName = fixtures.bucketName,
            objectKey = key
          )

          val future = fixtures.convertorService.runConversion(conversionJob)

          whenReady(future) { completedConversionJob =>

            val inputStream = s3Client
                .getObject(fixtures.bucketName, "target.txt.gz")
                .getObjectContent

              val sourceLines = scala.io.Source.fromInputStream(
                new GZIPInputStream(new BufferedInputStream(inputStream)))
                  .mkString.split("\n")

              val displayWorks = sourceLines.map(getResponseString => {
                val source: Json = (parse(getResponseString).right.get \\ "_source").head

                val displayWork = mapper.parse[DisplayWork](source.noSpaces)

                displayWork
              })

              all(displayWorks) shouldBe a[DisplayWork]

              displayWorks should contain theSameElementsAs expectedDisplayWorks
          }

      }
    }
  }
}
