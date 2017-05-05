package uk.ac.wellcome.elasticsearch.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.transport.RemoteTransportException
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.ElasticSearchLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

class RecordsIndexTest extends FunSpec with ElasticSearchLocal with ScalaFutures{
  // RecordsIndex is actually used in ElasticSearchLocal to create the index.
  // This tests class is to assert what can and cannot be done on an elasticsearch
  // node with the index mappings defined as in RecordIndex

  it("should be possible to index and retrieve a valid Work json") {
    val workJson = JsonUtil.toJson(IdentifiedWork(
      canonicalId = "1234",
      work = Work(identifiers = List(
        SourceIdentifier(source = "Miro",
          sourceId = "MiroID",
          value = "4321")),
        label = "this is the miro image label",
        accessStatus = None)
    )).get

    elasticClient.execute(
      indexInto(index / itemType).doc(workJson))

    eventually {
      val hits = elasticClient.execute(search(s"$index/$itemType").matchAll()).map { _.hits }.await
      hits should have size 1
      hits.head.sourceAsString shouldBe workJson
    }
  }

  it("it should fail inserting a document that does not match the mapping of a work") {
    val eventualIndexResponse = elasticClient.execute(
      indexInto(index / itemType).doc("""{"json":"json not matching the index structure"}"""))

    whenReady(eventualIndexResponse.failed) { exception =>
      exception shouldBe a [RemoteTransportException]
    }
  }
}
