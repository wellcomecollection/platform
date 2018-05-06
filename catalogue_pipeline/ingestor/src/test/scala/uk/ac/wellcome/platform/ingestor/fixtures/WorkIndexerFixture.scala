package uk.ac.wellcome.platform.ingestor.fixtures

import com.sksamuel.elastic4s.http.HttpClient
import org.scalatest.Suite
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.{MetricsSender => MetricsSenderFixture}
import uk.ac.wellcome.platform.ingestor.services.WorksIndexer
import uk.ac.wellcome.test.fixtures._

trait WorkIndexerFixture extends Akka with MetricsSenderFixture { this: Suite =>
  def withWorkIndexer[R](
    esType: String,
    elasticClient: HttpClient,
    metricsSender: MetricsSender)(testWith: TestWith[String, R]): R = {
    val workIndexer = new WorkIndexer(
      esType = esType,
      elasticClient = elasticClient,
      metricsSender = metricsSender
    )

    testWith(workIndexer)
  }

  def withWorkIndexerFixtures[R](esType: String, elasticClient: HttpClient)(testWith: TestWith[String, R]): R = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withWorkIndexer(esType, elasticClient, metricsSender) { workIndexer =>
          testWith(workIndexer)
        }
      }
    }
  }
}
