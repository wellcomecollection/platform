package uk.ac.wellcome.monitoring.test.fixtures

import akka.actor.ActorSystem
import com.twitter.inject.Logging
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.test.fixtures._

import scala.concurrent.Future

trait MetricsSenderFixture
    extends Logging
    with MockitoSugar
    with CloudWatch
    with Akka {

  def withMetricsSender[R](actorSystem: ActorSystem) =
    fixture[MetricsSender, R](
      create = new MetricsSender(
        namespace = awsNamespace,
        flushInterval = flushInterval,
        amazonCloudWatch = cloudWatchClient,
        actorSystem = actorSystem
      )
    )

  def withMockMetricSender[R] = fixture[MetricsSender, R](
    create = {
      val metricsSender = mock[MetricsSender]

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
