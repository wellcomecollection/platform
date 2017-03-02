package uk.ac.wellcome.platform.transformer.actors

import akka.actor.Actor
import com.gu.scanamo.ScanamoFree
import com.twitter.inject.Logging

case class ExampleRecord(identifier: String)


class DynamoCaseClassExtractorActor extends Actor with Logging {
  def receive = {
    case record: RecordMap => {
      ScanamoFree.read[ExampleRecord](record.value) match {
        case Right(rec) => {
          info(s"Parsed DynamoDB record ${rec}")
          // Send to next actor
        }
        case Left(rec) => {
          error(s"Unable to parse record ${rec}")
          // Send to dead letter queue or error
        }
      }
    }
    case event => error(s"Received unknown DynamoDB record ${event}")
  }
}
