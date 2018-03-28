package uk.ac.wellcome.test.fixtures

import akka.actor.ActorSystem
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

}
