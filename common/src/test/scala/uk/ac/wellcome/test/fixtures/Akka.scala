package uk.ac.wellcome.test.fixtures

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.concurrent.Eventually

trait Akka extends Eventually {

  def withActorSystem[R](testWith: TestWith[ActorSystem, R]) = {
    val actorSystem = ActorSystem()

    try {
      testWith(actorSystem)
    } finally {
      actorSystem.terminate()
      eventually { actorSystem.whenTerminated }
    }
  }

  def withMaterializer[R](actorSystem: ActorSystem)(
    testWith: TestWith[Materializer, R]): R = {
    val materializer = ActorMaterializer()(actorSystem)

    try {
      testWith(materializer)
    } finally {
      materializer.shutdown()
    }
  }

}
