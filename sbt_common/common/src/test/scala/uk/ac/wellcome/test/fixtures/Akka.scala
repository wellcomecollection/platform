package uk.ac.wellcome.test.fixtures

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import uk.ac.wellcome.fixtures._

import scala.concurrent.Await
import scala.concurrent.duration._

trait Akka {

  def withActorSystem[R]: Fixture[ActorSystem, R] = fixture[ActorSystem, R](
    create = ActorSystem(),
    destroy = system => Await.ready(system.terminate(), 10 seconds)
  )

  def withMaterializer[R](
    actorSystem: ActorSystem): Fixture[ActorMaterializer, R] =
    fixture[ActorMaterializer, R](
      create = ActorMaterializer()(actorSystem),
      destroy = _.shutdown()
    )

  def withMaterializer[R](testWith: TestWith[ActorMaterializer, R]): R =
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        testWith(materializer)
      }
    }
}
