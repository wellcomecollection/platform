package uk.ac.wellcome.utils

import akka.actor.ActorSystem
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.duration._

class TryBackoffTest
    extends FunSpec
    with BeforeAndAfterEach
    with Eventually
    with IntegrationPatience
    with Matchers {
  val system: ActorSystem = ActorSystem.create("TryBackoffTestActorSystem")

  val tryBackoff = new TryBackoff {
    var wasCalled: Boolean = false

    override lazy val totalWait = 6 seconds
    override def terminalFailureHook(): Unit = {
      wasCalled = true
    }

  }

  val discontinuousTryBackoff = new TryBackoff {
    override lazy val continuous = false
  }

  override def afterEach(): Unit = {
    println(s"canceling tryBackoff: ${tryBackoff.cancelRun()}")
    println(
      s"canceling discontinuousTryBackoff: ${discontinuousTryBackoff.cancelRun()}")
  }

  it("should always call a function that succeeds") {
    val f = new TryBackoffHelper()

    tryBackoff.run(f.alwaysSucceeds, system)
    eventually {
      f.calls shouldBe List(0)
    }
  }

  it("should recall a function after it fails on the first attempt") {
    val f = new TryBackoffHelper()

    tryBackoff.run(f.succeedsOnThirdAttempt, system)
    eventually {
      f.calls.length should be > 1
    }
  }

  it("should eventually give up on a function that always fails") {
    val f = new TryBackoffHelper()

    tryBackoff.run(f.alwaysFails, system)

    Thread.sleep(10000)
    val finalLength = f.calls.length
    Thread.sleep(5000)
    f.calls.length shouldBe finalLength
  }

  // it(
  //   "should eventually call terminalFailureHook for a a function that always fails") {
  //   val f = new TryBackoffHelper()
  //
  //   tryBackoff.wasCalled = false
  //   tryBackoff.run(f.alwaysFails, system)
  //
  //   eventually {
  //     tryBackoff.wasCalled shouldBe true
  //   }
  // }

  it("should stop after the first success if continuous is false") {
    val f = new TryBackoffHelper()

    discontinuousTryBackoff.run(f.alwaysSucceeds, system)
    eventually {
      f.calls.length shouldBe 1
    }
    Thread.sleep(1000)
    f.calls.length shouldBe 1
  }

  it("should recall a failing function function if continuous is false") {
    val f = new TryBackoffHelper()

    discontinuousTryBackoff.run(f.succeedsOnThirdAttempt, system)

    Thread.sleep(2000)
    f.calls.length shouldBe 3
  }

  it("should wait progressively longer between failed attempts") {
    val f = new TryBackoffHelper()

    tryBackoff.run(f.alwaysFails, system)
    Thread.sleep(10000)

    val differences = f.calls.reverse
      .sliding(2)
      .toList
      .map(ts => ts(1) - ts(0))

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

  // Methods passed to the TryBackoff.
  class TryBackoffHelper {
    var calls: List[Int] = List()

    def alwaysSucceeds(): Future[Unit] = {
      calls = 0 :: calls
      Future.successful(())
    }

    def alwaysFails(): Future[Unit] = {
      calls = System.currentTimeMillis().toInt :: calls
      Future.failed(new Exception("I will always fail"))
    }

    def succeedsOnThirdAttempt(): Future[Unit] = {
      if (calls.length < 2) {
        calls = 0 :: calls
        Future.failed(new Exception("Not ready yet"))
      } else {
        calls = 1 :: calls
        Future.successful(())
      }
    }
  }
}
