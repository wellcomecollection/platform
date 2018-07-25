package uk.ac.wellcome.monitoring.test

import java.time.Instant
import java.time.temporal.ChronoUnit

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest
import org.mockito.ArgumentCaptor
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.{MetricsConfig, MetricsSender}
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
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

  describe("count") {
    it("counts a successful future") {
      withActorSystem { actorSystem =>
        val amazonCloudWatch = mock[AmazonCloudWatch]
        val metricsConfig = MetricsConfig(
          namespace = "test",
          flushInterval = 1 second
        )
        val metricsSender = new MetricsSender(
          amazonCloudWatch = amazonCloudWatch,
          actorSystem = actorSystem,
          metricsConfig = metricsConfig
        )
        val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

        val expectedResult = "foo"
        val f = Future {
          expectedResult
        }
        val metricName = "bar"

        metricsSender.count(metricName, f)

        whenReady(f) { result =>
          result shouldBe expectedResult
          eventually {

            verify(amazonCloudWatch, times(1)).putMetricData(capture.capture())

            val putMetricDataRequest = capture.getValue
            val metricData = putMetricDataRequest.getMetricData
            metricData should have size 1
            metricData.asScala.exists { metricDatum =>
              (metricDatum.getValue == 1.0) && metricDatum.getMetricName == s"${metricName}_success"
            } shouldBe true
          }
        }
      }
    }

    it("counts a failed future") {
      withActorSystem { actorSystem =>
        val amazonCloudWatch = mock[AmazonCloudWatch]
        val metricsConfig = MetricsConfig(
          namespace = "test",
          flushInterval = 1 second
        )
        val metricsSender = new MetricsSender(
          amazonCloudWatch = amazonCloudWatch,
          actorSystem = actorSystem,
          metricsConfig = metricsConfig
        )
        val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

        val f = Future {
          throw new RuntimeException()
        }
        val metricName = "bar"

        metricsSender.count(metricName, f)

        whenReady(f.failed) { _ =>
          eventually {
            verify(amazonCloudWatch, times(1)).putMetricData(capture.capture())

            val putMetricDataRequest = capture.getValue
            val metricData = putMetricDataRequest.getMetricData
            metricData should have size 1

            metricData.asScala.exists { metricDatum =>
              (metricDatum.getValue == 1.0) && metricDatum.getMetricName == s"${metricName}_failure"
            } shouldBe true
          }
        }
      }
    }

    it("groups 20 MetricDatum into one PutMetricDataRequest") {
      withActorSystem { actorSystem =>
        val amazonCloudWatch = mock[AmazonCloudWatch]
        val metricsConfig = MetricsConfig(
          namespace = "test",
          flushInterval = 1 second
        )
        val metricsSender = new MetricsSender(
          amazonCloudWatch = amazonCloudWatch,
          actorSystem = actorSystem,
          metricsConfig = metricsConfig
        )

        val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

        val f = Future.successful(())
        val metricName = "bar"

        val futures =
          (1 to 40).map(i => metricsSender.count(s"$metricName", f))

        whenReady(Future.sequence(futures)) { _ =>
          eventually {
            verify(amazonCloudWatch, times(2)).putMetricData(capture.capture())

            val putMetricDataRequests = capture.getAllValues
            putMetricDataRequests should have size 2

            putMetricDataRequests.asScala.head.getMetricData should have size 20
            putMetricDataRequests.asScala.tail.head.getMetricData should have size 20
          }
        }
      }
    }

    it("takes at least one second to make 150 PutMetricData requests") {
      withActorSystem { actorSystem =>
        val amazonCloudWatch = mock[AmazonCloudWatch]
        val metricsConfig = MetricsConfig(
          namespace = "test",
          flushInterval = 2 seconds
        )
        val metricsSender = new MetricsSender(
          amazonCloudWatch = amazonCloudWatch,
          actorSystem = actorSystem,
          metricsConfig = metricsConfig
        )
        val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

        val f = Future.successful(())
        val metricName = "bar"

        val expectedDuration = (1 second).toMillis
        val startTime = Instant.now

        // Each PutMetricRequest is made of 20 MetricDatum so we need
        // 20 * 150 = 3000 calls to incrementCount to get 150 PutMetricData calls
        val futures =
          (1 to 3000).map(i => metricsSender.count(s"${i}_$metricName", f))

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
}
