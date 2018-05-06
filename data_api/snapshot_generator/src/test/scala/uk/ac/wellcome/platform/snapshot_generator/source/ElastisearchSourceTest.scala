package uk.ac.wellcome.platform.snapshot_generator.source

import akka.stream.scaladsl.Sink
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

class ElastisearchSourceTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with Akka
    with ElasticsearchFixtures
    with WorksUtil {

  it("outputs the entire content of the index") {
    val itemType = "work"
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { actorMaterialiser =>
        withLocalElasticsearchIndex(itemType = itemType) { indexName =>
          implicit val materialiser = actorMaterialiser
          val works = (1 to 10).map { i =>
            workWith(
              canonicalId = s"$i-id",
              title = "woah! a wise wizard with walnuts!")
          }
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
          val visibleWorks = createWorks(count = 10, visible = true)
          val invisibleWorks =
            createWorks(count = 3, start = 11, visible = false)

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
