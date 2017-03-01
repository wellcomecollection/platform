package uk.ac.wellcome.platform.transformer.actors

import akka.actor.{Actor, ActorSystem, Props}

import com.twitter.inject.Logging

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter

import com.gu.scanamo.ScanamoFree

case class CleanedRecord(identifier: String)

trait Transformable {
  def tranform[T]: CleanedRecord
}

case class ExampleRecord(identifier: String) extends Transformable {
  def transform[ExampleRecord](): CleanedRecord {
    // abracadabra goes here
    CleanedRecord(identifier)
  }
}


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
