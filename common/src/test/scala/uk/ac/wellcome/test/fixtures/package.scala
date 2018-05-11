package uk.ac.wellcome.test

import com.twitter.inject.Logging

import scala.util.Try

package object fixtures extends Logging {

  type TestWith[T, R] = T => R
  type Fixture[L, R] = TestWith[L, R] => R

  def safeCleanup[L](resource: L)(f: L => Unit): Unit = {
    Try {
      logger.debug(s"cleaning up resource=[$resource]")
      f(resource)
    } recover {
      case e =>
        logger.warn(
          s"error cleaning up resource=[$resource]",
          e
        )
    }
  }

  private val noop = (x: Any) => ()

  def fixture[L, R](create: => L, destroy: L => Unit = noop): Fixture[L, R] =
    (testWith: TestWith[L, R]) => {
      val loan = create
      logger.debug(s"created test resource=[$loan]")
      try {
        testWith(loan)
      } finally {
        safeCleanup(loan) { destroy(_) }
      }
    }
}
