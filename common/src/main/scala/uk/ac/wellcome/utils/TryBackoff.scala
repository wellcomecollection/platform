package uk.ac.wellcome.utils

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.twitter.inject.Logging

import scala.concurrent.duration.Duration
import scala.math.pow
import scala.util.{Failure, Success, Try}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

trait TryBackoff extends Logging {
  val baseWaitMillis = 100
  val maxAttempts = 75

  def run(f: (() => Unit), system: ActorSystem, attempt: Int = 0): Unit = {

    val numberOfAttempts = Try {
      f()
    } match {
      case Success(_) => 0
      case Failure(_) =>
        error(s"Failed to get new messages (attempt: $attempt)")
        attempt + 1
    }

    if (numberOfAttempts > maxAttempts)
      throw new RuntimeException("Max retry attempts exceeded")

    val waitTime =
      //if there are failures, we want to retry increasing the amount of time we wait each time
      if (attempt > 0) increaseWaitTimeExponentially(attempt)
      else baseWaitMillis

    system.scheduler.scheduleOnce(
      Duration.create(waitTime, TimeUnit.MILLISECONDS)
    )(run(f, system, numberOfAttempts))
  }

  private def increaseWaitTimeExponentially(attempt: Int) = {
    val exponent = attempt / (baseWaitMillis / 4)
    pow(baseWaitMillis, exponent).toLong
  }
}
