package uk.ac.wellcome.utils

import akka.actor.{ActorSystem, Cancellable}
import com.twitter.inject.Logging
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.duration._
import scala.math.pow
import scala.util.{Failure, Success, Try}

/** This trait implements an exponential backoff algorithm.  This is useful
 *  for wrapping an operation that is known to be flakey/unreliable.
 *
 *  If the operation fails, we try again, but we wait an increasing amount
 *  of time between failed attempts.  This means we don't:
 *
 *    - Overwhelm an underlying service which might be overwhelmed -- and
 *      thus make the problem worse
 *    - Waste our own resources trying to repeat an operation that is likely
 *      to fail
 *
 *  How quickly we back off is controlled by two attributes:
 *
 *      @param baseWait   how long should we wait after the first failure
 *      @param totalWait  how long should we wait before giving up
 *
 *  Additionally, the operation can run again after it succeeds (reverting
 *  to the base wait time), or give up after the first success.  This is
 *  controlled by a third attribute:
 *
 *      @param continuous      true if the operation should repeat, false
 *                             if it should return after the first success
 *
 *  For example, to wait 1 second after the first failure and give up after
 *  five minutes, we would set
 *
 *      baseWait = 1 second
 *      totalWait = 5 minutes
 *
 *  Reference: https://en.wikipedia.org/wiki/Exponential_backoff
 *
 */
trait TryBackoff extends Logging {
  val baseWait = 100 millis
  val totalWait = 30 minutes
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

    if (numberOfAttempts > maxAttempts) {
      throw new RuntimeException("Max retry attempts exceeded")
    }

    val waitTime = timeToWaitOnAttempt(attempt)

    if (numberOfAttempts == 0 || continuous) {
      val cancellable = system.scheduler.scheduleOnce(waitTime milliseconds)(
        run(f, system, attempt = numberOfAttempts))
      maybeCancellable = Some(cancellable)
    }
  }

  def cancelRun(): Unit = {
    maybeCancellable.fold(())(cancellable => cancellable.cancel())
  }

  /** Returns the maximum number of attempts we should try.
   *
   *  In general, the exact number of attempts is less important than how
   *  long we should wait before writing the operation off as failed.  We need
   *  to know how many attempts to try for internal bookkeeping, but the
   *  calculation is abstracted away from the caller.
   */
  private def maximumAttemptsToTry(): Int = {
    @tailrec
    def go(attempt: Int, totalMillis: Long): Int = {
      val newTotalMillis = totalMillis + timeToWaitOnAttempt(attempt)
      if (newTotalMillis > totalWait.toMillis) attempt
      else go(attempt + 1, newTotalMillis)
    }
    go(attempt = 0, totalMillis = 0)
  }

  /** Returns the time to wait after the nth failure.
   *
   *  @param attempt which attempt has just failed (zero-indexed)
   */
  private def timeToWaitOnAttempt(attempt: Int): Long = {
    // This choice of exponent is somewhat arbitrary.  All we require is
    // that later attempts wait longer than earlier attempts.
    val exponent = attempt / (baseWait.toMillis / 4)
    pow(baseWait.toMillis, 1 + exponent).toLong
  }
}
