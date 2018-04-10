package uk.ac.wellcome.test.fixtures

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.Eventually

trait Akka extends Eventually with ImplicitLogging {

  def withActorSystem[R] = fixture[ActorSystem, R](
    create = ActorSystem(),
    destroy = eventually { _.terminate() }
  )

  def withMaterializer[R](actorSystem: ActorSystem) =
    fixture[ActorMaterializer, R](
      create = ActorMaterializer()(actorSystem),
      destroy = _.shutdown()
    )

}
