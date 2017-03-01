package uk.ac.wellcome.platform.transformer.actors

import akka.actor.{Actor, ActorSystem, Props}

import com.twitter.inject.Logging

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter

import com.gu.scanamo.ScanamoFree

case class ExampleRecord(identifier: String)


class DynamoCaseClassExtractorActor extends Actor with Logging {
  def receive = {
    case record: RecordMap => {
      // TODO: Error handling
      val parsedRecord = ScanamoFree.read[ExampleRecord](record.value).right.get
      info(s"Parsed DynamoDB record ${parsedRecord}")
    }
    case event => error(s"Received unknown DynamoDB record ${event}")
  }
}
