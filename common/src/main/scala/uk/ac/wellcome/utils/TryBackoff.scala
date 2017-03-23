package uk.ac.wellcome.utils

import java.util.concurrent.TimeUnit

import scala.math.pow
import scala.util.{
  Try,
  Success,
  Failure
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import akka.actor.{ActorSystem}

import com.twitter.inject.Logging

trait TryBackoff extends Logging {
  val baseWaitMillis = 100
  val maxAttempts  = 75

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

    if(nextAttempt > maxAttempts)
      throw new RuntimeException("Max retry attempts exceeded")

    val waitTime = pow(baseWaitMillis, (attempt / (baseWaitMillis / 4))).toLong

    system.scheduler.scheduleOnce(
      Duration.create(waitTime, TimeUnit.MILLISECONDS)
    )(run(f, system, nextAttempt))
  }
}


