package uk.ac.wellcome.platform.transformer.actors

import akka.actor.{Actor, ActorSystem, Props}

import com.twitter.inject.Logging

import com.amazonaws.services.dynamodbv2.model._

import uk.ac.wellcome.platform.transformer.modules._

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter

case class RecordMap(value: java.util.Map[String, AttributeValue])

class KinesisDynamoRecordExtractorActor extends Actor with Logging {
  def receive = {
    case record: RecordAdapter => {
      val keys = record
        .getInternalObject()
        .getDynamodb()
        .getKeys()
      info(s"Received record ${keys}")
      KinesisWorker.actor2 ! RecordMap(keys)
    }
    case event => error(s"Received unknown Kinesis event ${event}")
  }
}
