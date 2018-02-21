package uk.ac.wellcome.elasticsearch

import org.elasticsearch.client.{Response, ResponseException}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.ElasticSearchLocal
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.util.Success

class ElasticsearchExceptionManagerTest
    extends FunSpec
    with Matchers
    with ElasticSearchLocal {

  it("extracts the errorType from an elasticsearch exception") {
    val resp = elasticClient
      .execute {
        search(s"not-existing-index/not-existing")
          .query(matchAllQuery())
      }

    val elasticsearchManager = new ElasticsearchExceptionManager {}

    whenReady(resp.failed) { ex =>
      elasticsearchManager.getErrorType(ex.asInstanceOf[ResponseException]) shouldBe Some(
        "index_not_found_exception")
    }
  }

}
