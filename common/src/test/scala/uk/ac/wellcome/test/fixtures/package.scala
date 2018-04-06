package uk.ac.wellcome.test

import grizzled.slf4j.Logger

import scala.util.Try

package object fixtures extends FixtureComposers {

  type TestWith[T, R] = T => R

  type Fixture[L, R] = TestWith[L, R] => R

  def safeCleanup[L](resource: L)(f: L => Unit)(implicit logger: Logger): Unit = {
    Try {
      logger.debug(s"cleaning up resource=[$resource]")
      f(resource)
    } recover {
      case e => logger.warn(s"error cleaning up resource=[$resource]", e)
    }
  }

  private val noop = (x: Any) => ()

  def fixture[L, R](create: => L, destroy: L => Unit = noop)(implicit logger: Logger): Fixture[L, R] =
    (testWith: TestWith[L, R]) => {
      val loan = create
      try {
        testWith(loan)
      } finally {
        safeCleanup(loan){ destroy(_) }
      }
    }

  implicit class ComposableFixture[L1, R](
    private val thisFixture: Fixture[L1, R]) {

    def and[T, L2](that: T)(
      implicit fc: FixtureComposer[L1, L2, T, R]): Fixture[L2, R] =
      fc.compose(thisFixture, that)

  }

}
