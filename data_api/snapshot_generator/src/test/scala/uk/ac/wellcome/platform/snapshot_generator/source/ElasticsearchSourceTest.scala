package uk.ac.wellcome.platform.snapshot_generator.source

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.sksamuel.elastic4s.Index
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

class ElasticsearchSourceTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with Akka
    with ElasticsearchFixtures
    with WorksGenerators {

  it("outputs the entire content of the index") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalWorksIndex { index =>
          val works = createIdentifiedWorks(count = 10)
          insertIntoElasticsearch(index, works: _*)

          withSource(index) { source =>
            val future = source.runWith(Sink.seq)

            whenReady(future) { result =>
              result should contain theSameElementsAs works
            }
          }
        }
      }
    }
  }

  it("filters non visible works") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalWorksIndex { index =>
          val visibleWorks = createIdentifiedWorks(count = 10)
          val invisibleWorks = createIdentifiedInvisibleWorks(count = 3)

          val works = visibleWorks ++ invisibleWorks
          insertIntoElasticsearch(index, works: _*)

          withSource(index) { source =>
            val future = source.runWith(Sink.seq)

            whenReady(future) { result =>
              result should contain theSameElementsAs visibleWorks
            }
          }
        }
      }
    }
  }

  private def withSource[R](index: Index)(
    testWith: TestWith[Source[IdentifiedWork, NotUsed], R])(
    implicit actorSystem: ActorSystem): R = {
    val source = ElasticsearchWorksSource(
      elasticClient = elasticClient,
      index = index
    )
    testWith(source)
  }
}
