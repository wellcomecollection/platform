package uk.ac.wellcome.elasticsearch.mappings

import java.util

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import org.elasticsearch.transport.RemoteTransportException
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.ElasticSearchLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.collection.JavaConversions._

class WorksIndexTest
    extends FunSpec
    with ElasticSearchLocal
    with ScalaFutures
    with Eventually
    with IntegrationPatience
    with Matchers
    with BeforeAndAfterEach {

  val indexName = "records"
  val itemType = "item"

  val worksIndex = new WorksIndex(elasticClient, indexName, itemType)

  override def beforeEach(): Unit = {
    if (elasticClient.execute(indexExists(indexName)).await.isExists){
      elasticClient.execute(deleteIndex(indexName)).await
    }
    super.beforeEach()
  }

  it("should create an index where it's possible to insert and retrieve a valid Work json") {
    createAndWaitIndexIsCreated()

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

    elasticClient.execute(indexInto(indexName / itemType).doc(workJson))

    eventually {
      val hits = elasticClient
        .execute(search(s"$indexName/$itemType").matchAllQuery())
        .map { _.hits }
        .await
      hits should have size 1
      hits.head.sourceAsString shouldBe workJson
    }
  }

  it("it should create an index where inserting a document that does not match the mapping of a work fails") {
    createAndWaitIndexIsCreated()

    val eventualIndexResponse = elasticClient.execute(
      indexInto(indexName / itemType)
        .doc("""{"json":"json not matching the index structure"}"""))

    whenReady(eventualIndexResponse.failed) { exception =>
      exception shouldBe a [RemoteTransportException]
    }
  }

  it("should update an already existing index with the mapping") {
    createIndexAndInsertDocument()

    worksIndex.create

    eventually {
      val mappings: Map[String, AnyRef] = elasticClient
        .execute(getMapping(indexName / itemType))
        .await
        .mappingFor(indexName / itemType)
        .getSourceAsMap
        .toMap
      mappings("properties")
        .asInstanceOf[util.Map[String, AnyRef]]
        .keys should contain("work")
    }

  }

  private def createIndexAndInsertDocument() = {
    val futureIndexWithDocument =
      for {
        _ <- elasticClient.execute(createIndex(indexName))
        _ <- elasticClient.execute(putMapping(indexName / itemType).dynamic(DynamicMapping.Strict).as(keywordField("canonicalId")))
        _ <- elasticClient.execute(indexInto(indexName / itemType).doc(
          """
            |{
            | "canonicalId": "1234"
            |}
          """.stripMargin))
      } yield ()

    futureIndexWithDocument.await
  }

  private def createAndWaitIndexIsCreated() = {
    worksIndex.create.await

    eventually {
      elasticClient.execute(index exists indexName).await.isExists should be (true)
    }
  }
}
