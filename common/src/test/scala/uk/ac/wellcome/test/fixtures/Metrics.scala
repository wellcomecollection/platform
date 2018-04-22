package uk.ac.wellcome.test.fixtures

import com.twitter.inject.Logging
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.metrics.MetricsSender

import scala.concurrent.Future


trait Metrics
  extends Logging
    with Eventually
    with ImplicitLogging
    with MockitoSugar
{

  def withMetricsSender[R](testWith: TestWith[MetricsSender, R]): R = {
    val metricsSender: MetricsSender = mock[MetricsSender]

    when(
      metricsSender
        .timeAndCount[Unit](anyString, any[() => Future[Unit]].apply)
    ).thenReturn(
      Future.successful(())
    )

    testWith(metricsSender)
  }
}
