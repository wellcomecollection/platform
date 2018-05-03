package uk.ac.wellcome.messaging.test.fixtures

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.twitter.inject.Logging
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics
import uk.ac.wellcome.test.fixtures.ImplicitLogging

import scala.concurrent.Future

trait MetricsSender
    extends Logging
    with ImplicitLogging
    with MockitoSugar
    with CloudWatch
    with Akka {

  def withMetricsSender[R](actorSystem: ActorSystem) =
    fixture[metrics.MetricsSender, R](
      create = new metrics.MetricsSender(
        awsNamespace,
        flushInterval,
        cloudWatchClient,
        actorSystem)
    )

  def withMockMetricSender[R] = fixture[metrics.MetricsSender, R](
    create = {
      val metricsSender = mock[metrics.MetricsSender]

      when(
        metricsSender
          .timeAndCount[Unit](anyString, any[() => Future[Unit]].apply)
      ).thenReturn(
        Future.successful(())
      )

      metricsSender
    }
  )

}
