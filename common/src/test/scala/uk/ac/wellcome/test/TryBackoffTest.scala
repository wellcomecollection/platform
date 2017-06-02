package uk.ac.wellcome.platform.common

import akka.actor.ActorSystem
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import scala.concurrent.duration._
import uk.ac.wellcome.utils.TryBackoff


class TryBackoffTest extends FunSpec with BeforeAndAfterEach with Eventually with IntegrationPatience with Matchers {
  val system = ActorSystem.create("TestActorSystem")

  var calls = List[Int]()

  val tryBackoff = new TryBackoff {
    override def totalWait = 6 seconds
  }

  val discontinuousTryBackoff = new TryBackoff {
    override def continuous = false
  }

  override def afterEach(): Unit = {
    calls = List()
    tryBackoff.cancelRun()
    discontinuousTryBackoff.cancelRun()
  }

  it("should always call a function that succeeds") {
    def alwaysSucceeds(): Unit = {
      calls = 0 :: calls
    }

    tryBackoff.run(alwaysSucceeds, system)
    eventually {
      calls shouldBe List(0)
    }
  }

  it("should recall a function after it fails on the first attempt") {
    def succeedsOnThirdAttempt(): Unit = {
      if (calls.length < 2) {
        calls = 0 :: calls
        throw new Exception("Not ready yet")
      } else {
        calls = 1 :: calls
      }
    }

    tryBackoff.run(succeedsOnThirdAttempt, system)
    eventually {
      calls.length should be > 1
    }
  }

  it("should eventually give up on a function that always fails") {
    def alwaysFails(): Unit = {
      calls = 0 :: calls
      throw new Exception("I will always fail")
    }

    tryBackoff.run(alwaysFails, system)

    Thread.sleep(10000)
    val finalLength = calls.length
    Thread.sleep(5000)
    calls.length shouldBe finalLength
  }

  it("should stop after the first success if continuous is false") {
    def alwaysSucceeds(): Unit = {
      calls = 0 :: calls
    }

    discontinuousTryBackoff.run(alwaysSucceeds, system)
    eventually {
      calls.length shouldBe 1
    }
    Thread.sleep(1000)
    calls.length shouldBe 1
  }
}
