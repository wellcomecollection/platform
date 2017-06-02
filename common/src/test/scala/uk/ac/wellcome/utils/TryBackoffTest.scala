package uk.ac.wellcome.utils

import akka.actor.ActorSystem
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import scala.concurrent.duration._
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.TryBackoff

class TryBackoffTest
    extends FunSpec
    with BeforeAndAfterEach
    with Eventually
    with IntegrationPatience
    with Matchers {
  val system = ActorSystem.create("TestActorSystem")

  var calls = List[Int]()

  val tryBackoff = new TryBackoff {
    override lazy val totalWait = 6 seconds
  }

  val discontinuousTryBackoff = new TryBackoff {
    override lazy val continuous = false
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

  it("should recall a failing function function if continuous is false") {
    def succeedsOnThirdAttempt(): Unit = {
      if (calls.length < 2) {
        calls = 0 :: calls
        throw new Exception("Not ready yet")
      } else {
        calls = 1 :: calls
      }
    }

    discontinuousTryBackoff.run(succeedsOnThirdAttempt, system)

    Thread.sleep(2000)
    calls.length shouldBe 3
  }

  it("should wait progressively longer between failed attempts") {
    def alwaysFails(): Unit = {
      calls = System.currentTimeMillis().toInt :: calls
      throw new Exception("Failure is inevitable")
    }

    system.scheduler.scheduleOnce(5 milliseconds)(println("hello world"))
    Thread.sleep(25)

    tryBackoff.run(alwaysFails, system)
    Thread.sleep(10000)
    calls = calls.reverse

    val differences = calls.sliding(2).toList.map(ts => ts(1) - ts(0))

    // When we run this test in isolation in IntelliJ, there's a warmup
    // penalty -- the second invocation takes an unusually long time to run.
    // We see differences of the form:
    //
    //     244, 112, 144, 158, ...
    //
    // We don't see the warmup penalty if we run the whole suite.
    //
    // For now, we just drop the first difference -- the patttern is more
    // important than an individual element.
    differences.tail shouldBe sorted
  }
}
