package uk.ac.wellcome.platform.calm_adapter.modules

import java.nio.charset.{ Charset => JCharset }
import java.util.{ List => JList }
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{ActorSystem, Props}
import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions._
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.twitter.inject.{Injector, TwitterModule}

import uk.ac.wellcome.platform.calm_adapter.actors._


object CalmAdapterWorker extends TwitterModule {

  val system = ActorSystem("CalmAdapterWorker")
  val oaiHarvestActor         = system.actorOf(Props[OaiHarvestActor])
  val oaiParserActor          = system.actorOf(Props[OaiParserActor])
  val dynamoRecordWriterActor = system.actorOf(Props[DynamoRecordWriterActor])

  override def singletonStartup(injector: Injector) {
    info("Starting Adapter worker")

    val adapter = new AmazonDynamoDBStreamsAdapterClient(
      new DefaultAWSCredentialsProviderChain()
    )

    val system = injector.instance[ActorSystem]

    // TODO: Choose whether to do an OAI harvest or import from an S3 file.
    system.scheduler.scheduleOnce(
      Duration.create(50, TimeUnit.MILLISECONDS)
    )(calmAdapterStart())
  }

  def calmAdapterStart(): Unit = {
    oaiHarvestActor ! Map[String, String](
      // https://www.openarchives.org/OAI/openarchivesprotocol.html#ListRecords
      "verb" -> "ListRecords",

      // This format is defined in our OAI installation
      "metadataPrefix" -> "calm_xml"
    )
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Adapter worker")

    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
