package uk.ac.wellcome.sierra_adapter.services

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException

class WindowExtractorTest extends FunSpec with Matchers {

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

  it(
    "should return a GracefulFailureException if start is not a valid iso datetime") {
    val jsonString =
      s"""
         |{
         | "start": "blah",
         | "end": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor
      .extractWindow(jsonString)
      .failed
      .get shouldBe a[GracefulFailureException]
  }

  it(
    "should return a GracefulFailureException if start is a datetime but does not have a timezone") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-10T17:16:35",
         | "end": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor
      .extractWindow(jsonString)
      .failed
      .get shouldBe a[GracefulFailureException]
  }

  it(
    "should return a GracefulFailureException if end is not a valid iso datetime") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-13T21:34:35Z",
         | "end": "blah"
         |}
      """.stripMargin
    WindowExtractor
      .extractWindow(jsonString)
      .failed
      .get shouldBe a[GracefulFailureException]
  }

  it(
    "should return a GracefulFailureException if end is a datetime but does not have a timezone") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-10T17:16:35Z",
         | "end": "2013-12-13T21:34:35"
         |}
      """.stripMargin
    WindowExtractor
      .extractWindow(jsonString)
      .failed
      .get shouldBe a[GracefulFailureException]
  }

  it(
    "should return a GracefulFailureException if there is not a start datetime") {
    val jsonString =
      s"""
         |{
         | "end": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor
      .extractWindow(jsonString)
      .failed
      .get shouldBe a[GracefulFailureException]
  }

  it(
    "should return a GracefulFailureException if there is not an end datetime") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor
      .extractWindow(jsonString)
      .failed
      .get shouldBe a[GracefulFailureException]
  }

  it(
    "should return a GracefulFailureException if the start time is after the end time") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-14T21:34:35Z",
         | "end": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor
      .extractWindow(jsonString)
      .failed
      .get shouldBe a[GracefulFailureException]
  }

  it(
    "should return a GracefulFailureException if the start time is equal to the end time") {
    val jsonString =
      s"""
         |{
         | "start": "2013-12-13T21:34:35Z",
         | "end": "2013-12-13T21:34:35Z"
         |}
      """.stripMargin
    WindowExtractor
      .extractWindow(jsonString)
      .failed
      .get shouldBe a[GracefulFailureException]
  }
}
