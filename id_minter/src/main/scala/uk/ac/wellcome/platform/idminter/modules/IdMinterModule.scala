package uk.ac.wellcome.platform.idminter.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.{IdentifiedWork, Work}
import uk.ac.wellcome.platform.idminter.steps.{
  IdentifierGenerator,
  WorkExtractor
}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.utils.{JsonUtil, TryBackoff}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

object IdMinterModule extends TwitterModule with TryBackoff {
  val snsSubject = "identified-item"

  override def singletonStartup(injector: Injector) {
    info("Starting IdMinter module")

    val sqsReader = injector.instance[SQSReader]
    val idGenerator = injector.instance[IdentifierGenerator]
    val snsWriter = injector.instance[SNSWriter]
    val actorSystem = injector.instance[ActorSystem]
    run(() => start(sqsReader, idGenerator, snsWriter), actorSystem)
  }

  private def start(sqsReader: SQSReader,
                    idGenerator: IdentifierGenerator,
                    snsWriter: SNSWriter) = {

    sqsReader.retrieveAndDeleteMessages { message =>
      for {
        work <- WorkExtractor.toWork(message)
        canonicalId <- idGenerator.generateId(work)
        _ <- snsWriter.writeMessage(
          toIdentifiedWorkJson(work, canonicalId),
          Some(snsSubject))
      } yield ()
    }
  }

  private def toIdentifiedWorkJson(work: Work,
                                          canonicalId: String) = {
    JsonUtil.toJson(IdentifiedWork(canonicalId, work)).get
  }
}
