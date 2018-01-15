package uk.ac.wellcome.platform.ingestor.models

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import org.elasticsearch.client.ResponseException
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifierSchemes, _}
import uk.ac.wellcome.platform.ingestor.test.utils.ElasticSearchLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

class WorksIndexTest
    extends FunSpec
    with ElasticSearchLocal
    with ScalaFutures
    with Eventually
    with Matchers
    with BeforeAndAfterEach {

  val indexName = "records"
  val itemType = "item"

  val worksIndex = new WorksIndex(elasticClient, indexName, itemType)

  override def beforeEach(): Unit = {
    ensureIndexDeleted(indexName)
  }

  ignore("puts a work") {

  }
}
