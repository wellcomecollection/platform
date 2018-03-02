package uk.ac.wellcome.test.fixtures

import akka.actor.ActorSystem
import org.scalatest.concurrent.Eventually

trait AkkaFixtures extends Eventually {

  def withActorSystem(testWith: TestWith[ActorSystem]) = {
    val actorSystem = ActorSystem()

    try {
      testWith(actorSystem)
    } finally {
      eventually { actorSystem.terminate() }
    }
  }

}
