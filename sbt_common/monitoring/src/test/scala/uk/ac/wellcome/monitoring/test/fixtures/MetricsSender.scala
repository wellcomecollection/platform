package uk.ac.wellcome.monitoring.test.fixtures

import akka.actor.ActorSystem
import com.twitter.inject.Logging
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.monitoring
import uk.ac.wellcome.test.fixtures._

import scala.concurrent.Future

trait MetricsSender
  extends Logging
    with ImplicitLogging
    with MockitoSugar
    with CloudWatch
    with Akka {

  def withMetricsSender[R](actorSystem: ActorSystem) =
    fixture[monitoring.MetricsSender, R](
      create = new monitoring.MetricsSender(
        awsNamespace,
        flushInterval,
        cloudWatchClient,
        actorSystem)
    )

  def withMockMetricSender[R] = fixture[monitoring.MetricsSender, R](
    create = {
      val metricsSender = mock[monitoring.MetricsSender]

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
