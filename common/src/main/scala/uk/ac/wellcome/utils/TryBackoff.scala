package uk.ac.wellcome.utils

import akka.actor.{ActorSystem, Cancellable}
import com.twitter.inject.Logging
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.duration._
import scala.math.pow
import scala.util.{Failure, Success, Try}
import scala.util.control.Breaks.break

/** This trait implements an exponential backoff algorithm.  This is useful
  * for wrapping an operation that is known to be flakey/unreliable.
  *
  * If the operation fails, we try again, but we wait an increasing amount
  * of time between failed attempts.  This means we don't:
  *
  *   - Overwhelm an underlying service which might be overwhelmed -- and
  *     thus make the problem worse
  *   - Waste our own resources trying to repeat an operation that is likely
  *     to fail
  *
  * How quickly we back off is controlled by two attributes:
  *
  *     @param baseWaitMillis  how many milliseconds should we wait after
  *                            the first failure
  *     @param totalWaitMillis how many milliseconds should we wait before
  *                            giving up on the operation
  *
  * Additionally, the operation can run again after it succeeds (reverting
  * to the base wait time), or give up after the first success.  This is
  * controlled by a third attribute:
  *
  *     @param continuous      true if the operation should repeat, false
  *                            if it should return after the first success
  *
  * For example, to wait 1 second after the first failure and give up after
  * five minutes, we would set
  *
  *     baseWaitMillis = 1000
  *     totalWaitMillis = 5 * 60 * 1000
  *
  * Reference: https://en.wikipedia.org/wiki/Exponential_backoff
  *
  */
trait TryBackoff extends Logging {
  val baseWaitMillis = 100
  val totalWaitMillis = 30 * 60 * 1000 // half an hour
  val continuous = true

  // This value is cached to save us repeating the calculation.
  private val maxAttempts = maximumAttemptsToTry()

  private var maybeCancellable: Option[Cancellable] = None

  def run(f: (() => Unit), system: ActorSystem, attempt: Int = 0): Unit = {

    val numberOfAttempts = Try {
      f()
    } match {
      case Success(_) => 0
      case Failure(e) =>
        error(s"Failed to run (attempt: $attempt)", e)
        attempt + 1
    }

    // If we're in non-continuous mode and the last attempt succeeded,
    // we should return immediately.
    if (numberOfAttempts == 0 && !continuous) {
      return
    }

    if (numberOfAttempts > maxAttempts) {
      throw new RuntimeException("Max retry attempts exceeded")
    }

    val waitTime = timeToWaitOnAttempt(attempt)

    val cancellable = system.scheduler.scheduleOnce(waitTime milliseconds)(
      run(f, system, attempt = numberOfAttempts))
    maybeCancellable = Some(cancellable)
  }

  def cancelRun(): Unit = {
    maybeCancellable.fold(())(cancellable => cancellable.cancel())
  }

  /** Returns the maximum number of attempts we should try.
    *
    * In general, the exact number of attempts is less important than how
    * long we should wait before writing the operation off as failed.  We need
    * to know how many attempts to try for internal bookkeeping, but the
    * calculation is abstracted away from the caller.
    */
  private def maximumAttemptsToTry(): Int = {
    var totalMillis: Long = 0
    var attempt = 0
    while (true) {
      totalMillis = totalMillis + timeToWaitOnAttempt(attempt)
      if (totalMillis > totalWaitMillis) {
        break
      } else {
        attempt += 1
      }
    }
    attempt
  }

  /** Returns the time to wait after the nth failure.
    *
    * @param attempt which attempt has just failed (zero-indexed)
    */
  private def timeToWaitOnAttempt(attempt: Int): Long = {
    // This choice of exponent is somewhat arbitrary.  All we require is
    // that later attempts wait longer than earlier attempts.
    val exponent = attempt / (baseWaitMillis / 4)
    pow(baseWaitMillis, 1 + exponent).toLong
  }
}
