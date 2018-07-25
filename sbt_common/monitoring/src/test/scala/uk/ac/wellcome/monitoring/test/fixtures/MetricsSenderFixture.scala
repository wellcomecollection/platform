package uk.ac.wellcome.monitoring.test.fixtures

import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import org.mockito.Matchers.{any, anyString, endsWith}
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.monitoring.{MetricsConfig, MetricsSender}
import uk.ac.wellcome.test.fixtures._

import scala.concurrent.{ExecutionContext, Future}

trait MetricsSenderFixture
    extends Logging
    with MockitoSugar
    with CloudWatch
    with Akka {

  val QUEUE_RETRIES = 3

  def withMetricsSender[R](actorSystem: ActorSystem) =
    fixture[MetricsSender, R](
      create = new MetricsSender(
        amazonCloudWatch = cloudWatchClient,
        actorSystem = actorSystem,
        metricsConfig = MetricsConfig(
          namespace = awsNamespace,
          flushInterval = flushInterval
        )
      )
    )

  def withMockMetricSender[R] = fixture[MetricsSender, R](
    create = {
      val metricsSender = mock[MetricsSender]
      when(
        metricsSender.count(
          anyString(),
          any[Future[Unit]]()
        )(any[ExecutionContext])
      ).thenAnswer(new Answer[Future[Unit]] {
        override def answer(invocation: InvocationOnMock): Future[Unit] = {
          invocation.callRealMethod().asInstanceOf[Future[Unit]]
        }
      })
      metricsSender
    }
  )

  def assertSuccessMetricIncremented(mockMetricsSender: MetricsSender) = {
    verify(mockMetricsSender, times(1))
      .incrementCount(endsWith("_ProcessMessage_success"))
  }

  def assertFailureMetricIncremented(mockMetricsSender: MetricsSender) = {
    verify(mockMetricsSender, times(QUEUE_RETRIES))
      .incrementCount(endsWith("_ProcessMessage_failure"))
  }

  def assertFailureMetricNotIncremented(mockMetricsSender: MetricsSender) = {
    verify(mockMetricsSender, never())
      .incrementCount(endsWith("_ProcessMessage_failure"))
  }

  def assertGracefulFailureMetricIncremented(
    mockMetricsSender: MetricsSender) = {
    verify(mockMetricsSender, times(QUEUE_RETRIES))
      .incrementCount(endsWith("_gracefulFailure"))
  }

}
