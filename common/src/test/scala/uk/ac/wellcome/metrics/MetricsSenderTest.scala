package uk.ac.wellcome.metrics

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest
import org.mockito.ArgumentCaptor
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._

class MetricsSenderTest
    extends FunSpec
    with MockitoSugar
    with Matchers
    with ScalaFutures
    with Eventually {

  import org.mockito.Matchers.any
  import org.mockito.Mockito._

  describe("timeAndCount") {
    it("should record the time and count of a successful future") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender = new MetricsSender("test", amazonCloudWatch)
      val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

      val expectedResult = "foo"
      val timedFunction = () => Future { Thread.sleep(100); expectedResult }
      val metricName = "bar"

      val future = metricsSender.timeAndCount(metricName, timedFunction)

      whenReady(future) { result =>
        result shouldBe expectedResult
        eventually {

          verify(amazonCloudWatch, times(2)).putMetricData(capture.capture())

          capture.getAllValues.exists { request: PutMetricDataRequest =>
            val item = request.getMetricData()
            (item.head.getValue == 1.0) && item.head.getMetricName == "success"
          } shouldBe true

          capture.getAllValues.exists { request: PutMetricDataRequest =>
            val item = request.getMetricData()
            (item.head.getValue >= 100) && (item.head.getMetricName == "bar")
          } shouldBe true
        }
      }
    }

    it("should record the time and count of a failed future") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender = new MetricsSender("test", amazonCloudWatch)
      val capture = ArgumentCaptor.forClass(classOf[PutMetricDataRequest])

      val timedFunction = () => Future { throw new RuntimeException() }
      val metricName = "bar"

      val future = metricsSender.timeAndCount(metricName, timedFunction)

      whenReady(future.failed) { _ =>
        eventually {
          verify(amazonCloudWatch, times(2)).putMetricData(capture.capture())

          capture.getAllValues.exists { request: PutMetricDataRequest =>
            val item = request.getMetricData
            (item.head.getValue == 1.0) && item.head.getMetricName == "failure"
          } shouldBe true

          capture.getAllValues.exists { request: PutMetricDataRequest =>
            request.getMetricData.head.getMetricName == "bar"
          } shouldBe true
        }
      }
    }
  }

  describe("incrementCount") {
    it("should putMetricData with the correct value") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender = new MetricsSender("test", amazonCloudWatch)
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

    it(
      "should return a failed future when AmazonCloudWatch throws an exception") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender = new MetricsSender("test", amazonCloudWatch)
      val expectedException = new RuntimeException("bar")

      when(amazonCloudWatch.putMetricData(any(classOf[PutMetricDataRequest])))
        .thenThrow(expectedException)

      val actual = metricsSender.incrementCount("foo")

      whenReady(actual.failed) { actualException =>
        actualException shouldBe expectedException
      }
    }
  }

  describe("sendTime") {
    it("should putMetricData with the correct value") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender = new MetricsSender("test", amazonCloudWatch)
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

    it(
      "should return a failed future when AmazonCloudWatch throws an exception") {
      val amazonCloudWatch = mock[AmazonCloudWatch]
      val metricsSender = new MetricsSender("test", amazonCloudWatch)
      val expectedException = new RuntimeException("bar")

      when(amazonCloudWatch.putMetricData(any(classOf[PutMetricDataRequest])))
        .thenThrow(expectedException)

      val actual = metricsSender.sendTime("foo", 5 millis)

      whenReady(actual.failed) { actualException =>
        actualException shouldBe expectedException
      }
    }

  }
}
