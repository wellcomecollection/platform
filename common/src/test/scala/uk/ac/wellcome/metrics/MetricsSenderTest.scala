package uk.ac.wellcome.metrics

import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest
import org.mockito.ArgumentCaptor
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.collection.JavaConversions._
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

class MetricsSenderTest
    extends FunSpec
    with MockitoSugar
    with Matchers
    with ScalaFutures
    with Eventually
    with ExtendedPatience {

  import org.mockito.Mockito._

  val actorSystem = ActorSystem()

  describe("timeAndCount") {
    it("should record the time and count of a successful future") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender =
        new MetricsSender("test", 1 second, amazonCloudWatch, actorSystem)
      val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

      val expectedResult = "foo"
      val timedFunction = () => Future { Thread.sleep(100); expectedResult }
      val metricName = "bar"

      val future = metricsSender.timeAndCount(metricName, timedFunction)

      whenReady(future) { result =>
        result shouldBe expectedResult
        eventually {

          verify(amazonCloudWatch, times(1)).putMetricData(capture.capture())

          val putMetricDataRequest = capture.getValue
          val metricData = putMetricDataRequest.getMetricData
          metricData should have size 2
          metricData.exists { metricDatum =>
            (metricDatum.getValue == 1.0) && metricDatum.getMetricName == "success"
          } shouldBe true

          metricData.exists { metricDatum =>
            (metricDatum.getValue >= 100) && (metricDatum.getMetricName == "bar")
          } shouldBe true
        }
      }
    }

    it("should record the time and count of a failed future") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender =
        new MetricsSender("test", 1 second, amazonCloudWatch, actorSystem)
      val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

      val timedFunction = () => Future { throw new RuntimeException() }
      val metricName = "bar"

      val future = metricsSender.timeAndCount(metricName, timedFunction)

      whenReady(future.failed) { _ =>
        eventually {
          verify(amazonCloudWatch, times(1)).putMetricData(capture.capture())

          val putMetricDataRequest = capture.getValue
          val metricData = putMetricDataRequest.getMetricData
          metricData should have size 2

          metricData.exists { metricDatum =>
            (metricDatum.getValue == 1.0) && metricDatum.getMetricName == "failure"
          } shouldBe true

          metricData.exists { metricDatum =>
            metricDatum.getMetricName == "bar"
          } shouldBe true
        }
      }
    }

    it("should group 20 MetricDatum into one PutMetricDataRequest") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender =
        new MetricsSender("test", 1 second, amazonCloudWatch, actorSystem)
      val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

      val emptyFunction = () => Future.successful(())
      val metricName = "bar"

      val futures = (1 to 15).map(i =>
        metricsSender.timeAndCount(s"${i}_$metricName", emptyFunction))

      whenReady(Future.sequence(futures)) { _ =>
        eventually {
          verify(amazonCloudWatch, times(2)).putMetricData(capture.capture())

          val putMetricDataRequests = capture.getAllValues
          putMetricDataRequests should have size 2

          putMetricDataRequests.head.getMetricData should have size 20
          putMetricDataRequests.tail.head.getMetricData should have size 10
        }
      }
    }

    it("should take at least one second to make 150 PutMetricData requests") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender =
        new MetricsSender("test", 2 second, amazonCloudWatch, actorSystem)
      val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

      val expectedDuration = (1 second).toMillis
      val startTime = Instant.now

      // Each PutMetricRequest is made of 20 MetricDatum so we need
      // 20 * 150 = 3000 calls to incrementCount to get 150 PutMetricData calls
      val futures = (1 to 3000).map(i => metricsSender.incrementCount("bar"))

      val promisedInstant = Promise[Instant]

      whenReady(Future.sequence(futures)) { _ =>
        eventually {
          verify(amazonCloudWatch, times(150)).putMetricData(capture.capture())

          val putMetricDataRequests = capture.getAllValues

          putMetricDataRequests should have size 150

          promisedInstant.success(Instant.now())
        }
      }

      whenReady(promisedInstant.future) { endTime =>
        val gap: Long = ChronoUnit.MILLIS.between(startTime, endTime)
        gap shouldBe >(expectedDuration)
      }
    }
  }

  describe("incrementCount") {
    it("should putMetricData with the correct value") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender =
        new MetricsSender(
          "test",
          100 millisecond,
          amazonCloudWatch,
          actorSystem)
      val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

      val expectedValue = 3.0F

      val metricFuture = metricsSender.incrementCount("foo", expectedValue)

      whenReady(metricFuture) { _ =>
        eventually {
          verify(amazonCloudWatch).putMetricData(capture.capture())
          capture.getValue.getMetricData.head.getValue shouldBe expectedValue
        }
      }
    }
  }

  describe("sendTime") {
    it("should putMetricData with the correct value") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender =
        new MetricsSender(
          "test",
          100 milliseconds,
          amazonCloudWatch,
          actorSystem)
      val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

      val expectedValue = 5 millis

      val metricFuture = metricsSender.sendTime("foo", expectedValue)

      whenReady(metricFuture) { _ =>
        eventually {
          verify(amazonCloudWatch).putMetricData(capture.capture())
          capture.getValue.getMetricData.head.getValue shouldBe expectedValue.length
        }
      }
    }

  }
}
