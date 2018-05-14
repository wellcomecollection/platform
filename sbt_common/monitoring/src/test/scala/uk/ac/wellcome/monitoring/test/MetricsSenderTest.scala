package uk.ac.wellcome.monitoring.test

import java.time.Instant
import java.time.temporal.ChronoUnit

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest
import org.mockito.ArgumentCaptor
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

class MetricsSenderTest
    extends FunSpec
    with MockitoSugar
    with Matchers
    with ScalaFutures
    with Eventually
    with Akka
    with ExtendedPatience {

  import org.mockito.Mockito._

  describe("timeAndCount") {
    it("records the time and count of a successful future") {
      withActorSystem { actorSystem =>
        val amazonCloudWatch = mock[AmazonCloudWatch]
        val metricsSender = new MetricsSender(
          flushInterval = 1 second,
          amazonCloudWatch = amazonCloudWatch,
          actorSystem = actorSystem
        )
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
            metricData.asScala.exists { metricDatum =>
              (metricDatum.getValue == 1.0) && metricDatum.getMetricName == "success"
            } shouldBe true

            metricData.asScala.exists { metricDatum =>
              (metricDatum.getValue >= 100) && (metricDatum.getMetricName == "bar")
            } shouldBe true
          }
        }
      }
    }

    it("records the time and count of a failed future") {
      withActorSystem { actorSystem =>
        val amazonCloudWatch = mock[AmazonCloudWatch]
        val metricsSender = new MetricsSender(
          flushInterval = 1 second,
          amazonCloudWatch = amazonCloudWatch,
          actorSystem = actorSystem
        )
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

            metricData.asScala.exists { metricDatum =>
              (metricDatum.getValue == 1.0) && metricDatum.getMetricName == "failure"
            } shouldBe true

            metricData.asScala.exists { metricDatum =>
              metricDatum.getMetricName == "bar"
            } shouldBe true
          }
        }
      }
    }

    it("groups 20 MetricDatum into one PutMetricDataRequest") {
      withActorSystem { actorSystem =>
        val amazonCloudWatch = mock[AmazonCloudWatch]
        val metricsSender = new MetricsSender(
          flushInterval = 1 second,
          amazonCloudWatch = amazonCloudWatch,
          actorSystem = actorSystem
        )
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

            putMetricDataRequests.asScala.head.getMetricData should have size 20
            putMetricDataRequests.asScala.tail.head.getMetricData should have size 10
          }
        }
      }
    }

    it("takes at least one second to make 150 PutMetricData requests") {
      withActorSystem { actorSystem =>
        val amazonCloudWatch = mock[AmazonCloudWatch]
        val metricsSender = new MetricsSender(
          flushInterval = 2 seconds,
          amazonCloudWatch = amazonCloudWatch,
          actorSystem = actorSystem
        )
        val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

        val expectedDuration = (1 second).toMillis
        val startTime = Instant.now

        // Each PutMetricRequest is made of 20 MetricDatum so we need
        // 20 * 150 = 3000 calls to incrementCount to get 150 PutMetricData calls
        val futures = (1 to 3000).map(i => metricsSender.incrementCount("bar"))

        val promisedInstant = Promise[Instant]

        whenReady(Future.sequence(futures)) { _ =>
          eventually {
            verify(amazonCloudWatch, times(150))
              .putMetricData(capture.capture())

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
  }

  describe("incrementCount") {
    it("calls putMetricData with the correct value") {
      withActorSystem { actorSystem =>
        val amazonCloudWatch = mock[AmazonCloudWatch]
        val metricsSender = new MetricsSender(
          flushInterval = 100 milliseconds,
          amazonCloudWatch = amazonCloudWatch,
          actorSystem = actorSystem
        )
        val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

        val expectedValue = 3.0F

        val metricFuture = metricsSender.incrementCount("foo", expectedValue)

        whenReady(metricFuture) { _ =>
          eventually {
            verify(amazonCloudWatch).putMetricData(capture.capture())
            capture.getValue.getMetricData.asScala.head.getValue shouldBe expectedValue
          }
        }
      }
    }
  }

  describe("sendTime") {
    it("calls putMetricData with the correct value") {
      withActorSystem { actorSystem =>
        val amazonCloudWatch = mock[AmazonCloudWatch]
        val metricsSender = new MetricsSender(
          flushInterval = 100 milliseconds,
          amazonCloudWatch = amazonCloudWatch,
          actorSystem = actorSystem
        )
        val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

        val expectedValue = 5 millis

        val metricFuture = metricsSender.sendTime("foo", expectedValue)

        whenReady(metricFuture) { _ =>
          eventually {
            verify(amazonCloudWatch).putMetricData(capture.capture())
            capture.getValue.getMetricData.asScala.head.getValue shouldBe expectedValue.length
          }
        }
      }
    }
  }
}
