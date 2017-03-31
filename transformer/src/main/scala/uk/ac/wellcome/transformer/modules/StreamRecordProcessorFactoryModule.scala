package uk.ac.wellcome.platform.transformer.modules

import java.util.{List => JList}
import javax.inject.Singleton
import scala.collection.JavaConverters._

import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.amazonaws.services.kinesis.model.Record
import com.google.inject.Provides
import com.twitter.inject.TwitterModule

import akka.actor.ActorRef
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.platform.transformer.modules._
import uk.ac.wellcome.models.ActorRegister

class StreamsRecordProcessor(client: AmazonDynamoDB,
                             receiver: Option[ActorRef]
) extends IRecordProcessor {

  case class ExampleRecord(identifier: String)

  override def initialize(shardId: String) = Unit
  override def shutdown(
    checkpointer: IRecordProcessorCheckpointer,
    reason: ShutdownReason
  ): Unit = {
    if (reason == ShutdownReason.TERMINATE) {
      checkpointer.checkpoint()
    }
  }

  override def processRecords(
    records: JList[Record],
    checkpointer: IRecordProcessorCheckpointer
  ): Unit = {
    records.asScala.foreach { record =>
      receiver.foreach(_ ! record)
    }
  }
}

class StreamsRecordProcessorFactory(
  dynamoClient: AmazonDynamoDB,
  actorRegister: ActorRegister
) extends IRecordProcessorFactory {

  override def createProcessor(): IRecordProcessor =
    new StreamsRecordProcessor(dynamoClient,
                               actorRegister.actors
                                 .get("kinesisDynamoRecordExtractorActor"))

}

object StreamsRecordProcessorFactoryModule extends TwitterModule {
  override val modules = Seq(ActorRegistryModule, DynamoClientModule)

  @Singleton
  @Provides
  def provideStreamsRecordProcessorFactory(
    dynamoClient: AmazonDynamoDB,
    actorRegister: ActorRegister
  ): StreamsRecordProcessorFactory =
    new StreamsRecordProcessorFactory(dynamoClient, actorRegister)
}
