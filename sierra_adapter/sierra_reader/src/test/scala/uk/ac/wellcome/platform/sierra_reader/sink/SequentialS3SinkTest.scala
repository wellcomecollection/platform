package uk.ac.wellcome.platform.sierra_reader.sink

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Sink, Source}
import io.circe.Json
import io.circe.parser._
import org.scalatest.compatible.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import uk.ac.wellcome.test.fixtures.{Akka, S3, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.collection.JavaConversions._
import scala.concurrent.Future

class SequentialS3SinkTest
    extends FunSpec
    with Matchers
    with S3
    with Akka
    with BeforeAndAfterAll
    with ScalaFutures
    with ExtendedPatience {

  private def withSink(actorSystem: ActorSystem,
                       bucketName: String,
                       keyPrefix: String,
                       offset: Int = 0)(
    testWith: TestWith[Sink[(Json, Long), Future[Done]], Assertion]) = {
    implicit val executionContext = actorSystem.dispatcher
    val sink = SequentialS3Sink(
      s3Client,
      bucketName = bucketName,
      keyPrefix = keyPrefix,
      offset = offset
    )

    testWith(sink)
  }

  it("puts a single JSON in S3") {
    val json = parse(s"""{"hello": "world"}""").right.get

    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>
        withSink(
          actorSystem = actorSystem,
          bucketName = bucketName,
          keyPrefix = "testA_") { sink =>
          withMaterializer(actorSystem) { materializer =>
            val futureDone = Source
              .single(json)
              .zipWithIndex
              .runWith(sink)(materializer)

            whenReady(futureDone) { _ =>
              val s3objects =
                s3Client.listObjects(bucketName).getObjectSummaries
              s3objects should have size 1
              s3objects.head.getKey() shouldBe "testA_0000.json"

              getJsonFromS3(bucketName, "testA_0000.json") shouldBe json
            }
          }
        }
      }
    }
  }

  it("puts multiple JSON bodies into S3") {
    val json0 = parse(s"""{"red": "orange"}""").right.get
    val json1 = parse(s"""{"orange": "yellow"}""").right.get
    val json2 = parse(s"""{"yellow": "green"}""").right.get

    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>
        withSink(
          actorSystem = actorSystem,
          bucketName = bucketName,
          keyPrefix = "testB_") { sink =>
          withMaterializer(actorSystem) { materializer =>
            val futureDone = Source(List(json0, json1, json2)).zipWithIndex
              .runWith(sink)(materializer)

            whenReady(futureDone) { _ =>
              val s3objects =
                s3Client.listObjects(bucketName).getObjectSummaries
              s3objects should have size 3
              s3objects.map {
                _.getKey()
              } shouldBe List(
                "testB_0000.json",
                "testB_0001.json",
                "testB_0002.json")

              getJsonFromS3(bucketName, "testB_0000.json") shouldBe json0
              getJsonFromS3(bucketName, "testB_0001.json") shouldBe json1
              getJsonFromS3(bucketName, "testB_0002.json") shouldBe json2
            }
          }
        }
      }
    }
  }

  it("uses the offset if provided") {
    val json0 = parse(s"""{"red": "orange"}""").right.get
    val json1 = parse(s"""{"orange": "yellow"}""").right.get
    val json2 = parse(s"""{"yellow": "green"}""").right.get

    withLocalS3Bucket { bucketName =>
      withActorSystem { actorSystem =>
        withSink(
          actorSystem = actorSystem,
          bucketName = bucketName,
          keyPrefix = "testC_",
          offset = 3) { sink =>
          withMaterializer(actorSystem) { materializer =>
            val futureDone = Source(List(json0, json1, json2)).zipWithIndex
              .runWith(sink)(materializer)

            whenReady(futureDone) { _ =>
              val s3objects =
                s3Client.listObjects(bucketName).getObjectSummaries
              s3objects.map {
                _.getKey()
              } shouldBe List(
                "testC_0003.json",
                "testC_0004.json",
                "testC_0005.json")

              getJsonFromS3(bucketName, "testC_0003.json") shouldBe json0
              getJsonFromS3(bucketName, "testC_0004.json") shouldBe json1
              getJsonFromS3(bucketName, "testC_0005.json") shouldBe json2
            }
          }
        }
      }
    }
  }
}
