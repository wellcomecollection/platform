package uk.ac.wellcome.platform.sierra_reader.modules

import javax.inject.Singleton

import akka.actor.ActorSystem
import com.google.inject.Provides
import com.twitter.inject.annotations.Flag
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.sierra_reader.flow.SierraResourceTypes
import uk.ac.wellcome.platform.sierra_reader.flow.SierraResourceTypes.{
  bibs,
  items
}
import uk.ac.wellcome.platform.sierra_reader.services.SierraReaderWorkerService

object SierraReaderModule extends TwitterModule {
  flag[String]("reader.resourceType", "Sierra resource type")
  flag[Int]("reader.batchSize", 50, "Number of records in a single json batch")
  flag[String]("sierra.apiUrl", "", "Sierra API url")
  flag[String]("sierra.oauthKey", "", "Sierra API oauth key")
  flag[String]("sierra.oauthSecret", "", "Sierra API oauth secret")
  flag[String](
    "sierra.fields",
    "",
    "List of fields to include in the Sierra API response")

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[SierraReaderWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Sierra Bibs to SNS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraReaderWorkerService]

    workerService.stop()
    system.terminate()
  }

  @Singleton
  @Provides
  def providesSierraResourceType(
    @Flag("reader.resourceType") resourceTypeString: String)
    : SierraResourceTypes.Value = {
    resourceTypeString match {
      case s: String if s == bibs.toString => bibs
      case s: String if s == items.toString => items
      case s: String =>
        throw new IllegalArgumentException(
          s"$s is not a valid Sierra resource type")
    }
  }
}
