package uk.ac.wellcome.test.utils

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics.MetricsSender

trait MetricsSenderLocal extends MockitoSugar {
  val metricsSender: MetricsSender = new MetricsSender(
    namespace = "reindex-service-test",
    mock[AmazonCloudWatch])
}
