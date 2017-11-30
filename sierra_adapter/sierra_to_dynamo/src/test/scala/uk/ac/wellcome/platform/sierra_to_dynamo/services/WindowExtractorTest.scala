package uk.ac.wellcome.platform.sierra_to_dynamo.services

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.SQSReaderGracefulException


class WindowExtractorTest extends FunSpec with Matchers{

  it("should extract a valid message window from a json string") {
    val start = "2013-12-10T17:16:35Z"
    val end = "2013-12-13T21:34:35Z"
    val jsonString =
      s"""
        |{
        | "start": "$start",
        | "end": "$end"
        |}
      """.stripMargin
    WindowExtractor.extractWindow(jsonString).get shouldBe s"[$start,$end]"

  }

  it("should return a SQSReaderGracefulException if start is not a valid iso datetime") {
    val jsonString =
      s"""
         |{
         | "start": "blah",
         | "end": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor.extractWindow(jsonString).failed.get shouldBe a [SQSReaderGracefulException]

  }

  it("should return a SQSReaderGracefulException if start is a datetime but does not have a timezone") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-10T17:16:35",
         | "end": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor.extractWindow(jsonString).failed.get shouldBe a [SQSReaderGracefulException]

  }

  it("should return a SQSReaderGracefulException if end is not a valid iso datetime") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-13T21:34:35Z",
         | "end": "blah"
         |}
      """.stripMargin
    WindowExtractor.extractWindow(jsonString).failed.get shouldBe a [SQSReaderGracefulException]

  }

  it("should return a SQSReaderGracefulException if end is a datetime but does not have a timezone") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-10T17:16:35Z",
         | "end": "2013-12-13T21:34:35"
         |}
      """.stripMargin
    WindowExtractor.extractWindow(jsonString).failed.get shouldBe a [SQSReaderGracefulException]

  }

  it("should return a SQSReaderGracefulException if there is not a start datetime") {
    val jsonString =
      s"""
         |{
         | "end": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor.extractWindow(jsonString).failed.get shouldBe a [SQSReaderGracefulException]

  }

  it("should return a SQSReaderGracefulException if there is not an end datetime") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor.extractWindow(jsonString).failed.get shouldBe a [SQSReaderGracefulException]

  }

  it("should return a SQSReaderGracefulException if the start time is after the end time") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-14T21:34:35Z",
         | "end": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor.extractWindow(jsonString).failed.get shouldBe a [SQSReaderGracefulException]

  }

  it("should return a SQSReaderGracefulException if the start time is equal to the end time") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-13T21:34:35Z",
         | "end": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor.extractWindow(jsonString).failed.get shouldBe a [SQSReaderGracefulException]

  }

}
