package uk.ac.wellcome.monitoring.test.fixtures

import akka.actor.ActorSystem
import com.twitter.inject.Logging
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
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
        metricsSender.timeAndCount(
          anyString(),
          any[() => Future[Unit]]()
        )
      ).thenAnswer(new Answer[Future[Unit]] {
        override def answer(invocation: InvocationOnMock): Future[Unit] = {
          invocation.callRealMethod().asInstanceOf[Future[Unit]]
        }
      })

      metricsSender
    }
  )

}
