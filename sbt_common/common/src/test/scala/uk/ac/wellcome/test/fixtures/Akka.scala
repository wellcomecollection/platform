package uk.ac.wellcome.test.fixtures

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration._

trait Akka {

  def withActorSystem[R] = fixture[ActorSystem, R](
    create = ActorSystem(),
    destroy = system => Await.ready(system.terminate(), 10 seconds)
  )

  def withMaterializer[R](actorSystem: ActorSystem) =
    fixture[ActorMaterializer, R](
      create = ActorMaterializer()(actorSystem),
      destroy = _.shutdown()
    )

}
