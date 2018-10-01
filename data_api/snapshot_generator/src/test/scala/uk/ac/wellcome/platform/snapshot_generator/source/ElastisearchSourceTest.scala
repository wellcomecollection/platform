package uk.ac.wellcome.platform.snapshot_generator.source

import akka.stream.scaladsl.Sink
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.test.fixtures.Akka

class ElastisearchSourceTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with Akka
    with ElasticsearchFixtures
    with WorksGenerators {

  it("outputs the entire content of the index") {
    val itemType = "work"
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { actorMaterialiser =>
        withLocalElasticsearchIndex(itemType = itemType) { indexName =>
          implicit val materialiser = actorMaterialiser
          val works = createIdentifiedWorks(count = 10)
          insertIntoElasticsearch(indexName, itemType, works: _*)

          val future =
            ElasticsearchWorksSource(elasticClient, indexName, itemType)(
              actorSystem).runWith(Sink.seq)

          whenReady(future) { result =>
            result should contain theSameElementsAs works
          }
        }
      }
    }
  }

  it("filters non visible works") {
    val itemType = "work"
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { actorMaterialiser =>
        withLocalElasticsearchIndex(itemType = itemType) { indexName =>
          implicit val materialiser = actorMaterialiser
          val visibleWorks = createIdentifiedWorks(count = 10)
          val invisibleWorks = createIdentifiedInvisibleWorks(count = 3)

          val works = visibleWorks ++ invisibleWorks
          insertIntoElasticsearch(indexName, itemType, works: _*)

          val future =
            ElasticsearchWorksSource(elasticClient, indexName, itemType)(
              actorSystem).runWith(Sink.seq)

          whenReady(future) { result =>
            result should contain theSameElementsAs visibleWorks
          }
        }
      }
    }
  }

}
