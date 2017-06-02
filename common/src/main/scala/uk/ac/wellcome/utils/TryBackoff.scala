package uk.ac.wellcome.utils

import akka.actor.{ActorSystem, Cancellable}
import com.twitter.inject.Logging
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.duration._
import scala.math.pow
import scala.util.{Failure, Success, Try}

trait TryBackoff extends Logging {
  val baseWaitMillis = 100
  val maxAttempts = 75
  val continuous: Boolean = true
  private var maybeCancellable: Option[Cancellable] = None

  def run(f: (() => Unit), system: ActorSystem, attempt: Int = 0): Unit = {

    val succeeded = Try { f() } match {
      case Success(_) => true
      case Failure(e) =>
        error(s"Failed to run (attempt: $attempt)", e)
        false
    }

    val numberOfAttempts = if (succeeded) { 0 } else { attempt + 1 }

    if (numberOfAttempts > maxAttempts)
      throw new RuntimeException("Max retry attempts exceeded")

    val waitTime =
      //if there are failures, we want to retry increasing the amount of time we wait each time
      if (attempt > 0) increaseWaitTimeExponentially(attempt)
      else baseWaitMillis

    if (continuous || !succeeded) {
      val cancellable = system.scheduler.scheduleOnce(waitTime milliseconds)(
        run(f, system, numberOfAttempts))

      maybeCancellable = Some(cancellable)
    }
  }

  def cancelRun(): Unit = {
    maybeCancellable.fold(())(cancellable => cancellable.cancel())
  }

  private def increaseWaitTimeExponentially(attempt: Int) = {
    val exponent = attempt / (baseWaitMillis / 4)
    pow(baseWaitMillis, exponent).toLong
  }
}
