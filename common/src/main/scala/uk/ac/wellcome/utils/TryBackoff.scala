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

    val nextAttemptTry: Try[Int] = Try {
      f(); 0
    } recoverWith {
      case e: Throwable => {
        error(s"Failed to get new messages (attempt: ${attempt})")
        Try(attempt + 1)
      }
    }

    val nextAttempt = nextAttemptTry match {
      case Success(attempt) => attempt
      case Failure(e) => throw e; 0
    }

    if (nextAttempt > maxAttempts)
      throw new RuntimeException("Max retry attempts exceeded")

    val exponent = attempt / (baseWaitMillis / 4)
    val waitTime = if(exponent != 0) pow(baseWaitMillis, exponent).toLong else baseWaitMillis

    system.scheduler.scheduleOnce(
      Duration.create(waitTime, TimeUnit.MILLISECONDS)
    )(run(f, system, nextAttempt))
  }
}
