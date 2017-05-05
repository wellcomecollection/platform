package uk.ac.wellcome.elasticsearch.mappings

import com.sksamuel.elastic4s.testkit.ElasticSugar
import org.elasticsearch.transport.RemoteTransportException
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.collection.JavaConversions._

class WorksIndexTest
    extends FunSpec
    with ElasticSugar
    with ScalaFutures
    with Eventually
    with IntegrationPatience
    with Matchers
    with BeforeAndAfterEach {

  val indexName = "records"
  val itemType = "item"

  val worksIndex = new WorksIndex(client, indexName, itemType)

  override def beforeEach(): Unit = {
    deleteIndex(indexName)
  }

  it("should create an index where it's possible to insert and retrieve a valid Work json") {
    createAndWaitIndexIsCreated

    val workJson = JsonUtil
      .toJson(
        IdentifiedWork(
          canonicalId = "1234",
          work = Work(identifiers = List(
                        SourceIdentifier(source = "Miro",
                                         sourceId = "MiroID",
                                         value = "4321")),
                      label = "this is the miro image label",
                      accessStatus = None)
        ))
      .get

    client.execute(indexInto(indexName / itemType).doc(workJson))

    eventually {
      val hits = client
        .execute(search(s"$indexName/$itemType").matchAll())
        .map { _.hits }
        .await
      hits should have size 1
      hits.head.sourceAsString shouldBe workJson
    }
  }

  it("it should create an index where inserting a document that does not match the mapping of a work fails") {
    createAndWaitIndexIsCreated

    val eventualIndexResponse = client.execute(
      indexInto(indexName / itemType)
        .doc("""{"json":"json not matching the index structure"}"""))

    whenReady(eventualIndexResponse.failed) { exception =>
      exception shouldBe a[RemoteTransportException]
    }
  }

  it("should update an already existing index with the mapping") {
    ensureIndexExists(indexName)

    worksIndex.create.await

    eventually {
      val mappings = client
        .execute(getMapping(indexName / itemType))
        .await
        .mappingFor(indexName / itemType)
        .getSourceAsMap
        .toMap
      mappings should contain("dynamic" -> "strict")
    }

  }

  private def createAndWaitIndexIsCreated = {
    worksIndex.create.await

    eventually {
      doesIndexExists(indexName) shouldBe true
    }
  }
}
