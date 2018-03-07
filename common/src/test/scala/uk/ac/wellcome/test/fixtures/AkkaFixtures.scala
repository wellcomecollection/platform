package uk.ac.wellcome.test.fixtures

import akka.actor.ActorSystem
import org.scalatest.concurrent.Eventually

trait AkkaFixtures extends Eventually {

  def withActorSystem[R](testWith: TestWith[ActorSystem, R]) = {
    val actorSystem = ActorSystem()

    try {
      testWith(actorSystem)
    } finally {
      eventually { actorSystem.terminate() }
    }
  }

}
