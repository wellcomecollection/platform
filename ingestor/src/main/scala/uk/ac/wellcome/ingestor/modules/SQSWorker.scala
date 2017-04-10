package uk.ac.wellcome.platform.ingestor.modules

import akka.actor.ActorSystem
import com.amazonaws.services.sqs.model.{Message => AwsSQSMessage}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.ingestor.services.MessageProcessorService
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.utils.TryBackoff
import uk.ac.wellcome.utils.GlobalExecutionContext.context

object SQSWorker extends TwitterModule with TryBackoff {

  def processMessages(
                       sqsReader: SQSReader,
                       messageProcessorService: MessageProcessorService
  ): Unit = {

    sqsReader.retrieveMessages().map(messages =>
      messages.map(messageProcessorService.processMessage))

  }

  override def singletonStartup(injector: Injector) {
    info("Starting SQS worker")

    val system = injector.instance[ActorSystem]

    val sqsReader = injector.instance[SQSReader]
    val messageProcessorService = injector.instance[MessageProcessorService]

    def start() =
      processMessages(sqsReader, messageProcessorService)

    run(start, system)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
