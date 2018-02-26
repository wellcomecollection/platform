package uk.ac.wellcome.platform.sierra_reader.sink

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import io.circe.parser._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import uk.ac.wellcome.test.utils.{ExtendedPatience, S3Local}

import scala.collection.JavaConversions._

class SequentialS3SinkTest
    extends FunSpec
    with Matchers
    with S3Local
    with BeforeAndAfterAll
    with ScalaFutures
    with ExtendedPatience {

  implicit val system = ActorSystem()
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  override def afterAll(): Unit = {
    system.terminate()
    materialiser.shutdown()
    super.afterAll()
  }

  override lazy val bucketName = "sequential-s3-sink-test"

  it("puts a single JSON in S3") {
    val sink =
      SequentialS3Sink(s3Client, bucketName = bucketName, keyPrefix = "testA_")

    val json = parse(s"""{"hello": "world"}""").right.get

    val futureDone = Source
      .single(json)
      .zipWithIndex
      .runWith(sink)

    whenReady(futureDone) { _ =>
      val s3objects = s3Client.listObjects(bucketName).getObjectSummaries
      s3objects should have size 1
      s3objects.head.getKey() shouldBe "testA_0000.json"

      getJsonFromS3(bucketName, "testA_0000.json") shouldBe json
    }
  }

  it("puts multiple JSON bodies into S3") {
    val sink =
      SequentialS3Sink(s3Client, bucketName = bucketName, keyPrefix = "testB_")

    val json0 = parse(s"""{"red": "orange"}""").right.get
    val json1 = parse(s"""{"orange": "yellow"}""").right.get
    val json2 = parse(s"""{"yellow": "green"}""").right.get

    val futureDone = Source(List(json0, json1, json2)).zipWithIndex
      .runWith(sink)

    whenReady(futureDone) { _ =>
      val s3objects = s3Client.listObjects(bucketName).getObjectSummaries
      s3objects should have size 3
      s3objects.map { _.getKey() } shouldBe List(
        "testB_0000.json",
        "testB_0001.json",
        "testB_0002.json")

      getJsonFromS3(bucketName, "testB_0000.json") shouldBe json0
      getJsonFromS3(bucketName, "testB_0001.json") shouldBe json1
      getJsonFromS3(bucketName, "testB_0002.json") shouldBe json2
    }
  }

  it("uses the offset if provided") {
    val sink = SequentialS3Sink(
      s3Client,
      bucketName = bucketName,
      keyPrefix = "testC_",
      offset = 3)

    val json0 = parse(s"""{"red": "orange"}""").right.get
    val json1 = parse(s"""{"orange": "yellow"}""").right.get
    val json2 = parse(s"""{"yellow": "green"}""").right.get

    val futureDone = Source(List(json0, json1, json2)).zipWithIndex
      .runWith(sink)

    whenReady(futureDone) { _ =>
      val s3objects = s3Client.listObjects(bucketName).getObjectSummaries
      s3objects.map { _.getKey() } shouldBe List(
        "testC_0003.json",
        "testC_0004.json",
        "testC_0005.json")

      getJsonFromS3(bucketName, "testC_0003.json") shouldBe json0
      getJsonFromS3(bucketName, "testC_0004.json") shouldBe json1
      getJsonFromS3(bucketName, "testC_0005.json") shouldBe json2
    }
  }
}
