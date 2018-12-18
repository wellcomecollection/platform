package uk.ac.wellcome.platform.archive.notifier.fixtures

import java.util.UUID

import akka.http.scaladsl.model._
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.notifier.models.CallbackFlowResult

import scala.util.{Failure, Success}

trait NotifierGenerators extends RandomThings {

  def createCallbackResultWith(id: UUID = randomUUID,
                               response: HttpResponse): CallbackFlowResult =
    CallbackFlowResult(id, Some(Success(response)))

  def createFailedCallbackResultWith(id: UUID = randomUUID,
                                     exception: Throwable): CallbackFlowResult =
    CallbackFlowResult(id, Some(Failure(exception)))

  def createEmptyCallbackResultWith(id: UUID = randomUUID): CallbackFlowResult =
    CallbackFlowResult(id, None)
}
