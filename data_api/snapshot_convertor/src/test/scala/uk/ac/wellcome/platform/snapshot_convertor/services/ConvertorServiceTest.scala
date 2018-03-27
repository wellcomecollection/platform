package uk.ac.wellcome.platform.snapshot_convertor.services

import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.snapshot_convertor.models.ConversionJob
import uk.ac.wellcome.test.fixtures.AkkaFixtures
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.ConvertorServiceFixture
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.ExampleDump

import io.circe.parser._

class ConvertorServiceTest
    extends FunSpec
    with ScalaFutures
    with AkkaFixtures
    with ExampleDump
    with Matchers
    with ExtendedPatience
    with ConvertorServiceFixture {

  it(
    "converts a gzipped elasticdump from S3 into the correct format in the target bucket") {

    withConvertorService { fixtures =>
      withExampleDump(fixtures.bucketName) { key =>

        val conversionJob = ConversionJob(
          bucketName = fixtures.bucketName,
          objectKey = key
        )

        val future = fixtures.convertorService.runConversion(conversionJob)

        whenReady(future) { completedConversionJob =>
          val inputStream = s3Client
            .getObject(fixtures.bucketName, "target.txt.gz")
            .getObjectContent

          val actualDisplayWorkJson = parse(
            scala.io.Source
              .fromInputStream(
                new GZIPInputStream(new BufferedInputStream(inputStream)))
              .mkString
              .split("\n")
              .head
          )

          actualDisplayWorkJson should be(parse(expectedDisplayWork))
        }

      }
    }
  }
}
