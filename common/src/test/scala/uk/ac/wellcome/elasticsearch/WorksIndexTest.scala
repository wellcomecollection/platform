package uk.ac.wellcome.elasticsearch

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.test.utils.ElasticSearchLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context

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
