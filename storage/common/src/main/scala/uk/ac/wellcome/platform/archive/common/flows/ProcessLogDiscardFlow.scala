package uk.ac.wellcome.platform.archive.common.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archive.common.progress.models.FailedEvent

import scala.util.Try

/** This process takes a function (In => Try[Out]), and only returns
  * the successful results.
  *
  */
object ProcessLogDiscardFlow {
  def apply[In, Out](name: String)(
    f: In => Try[Out]): Flow[In, Out, NotUsed] = {
    type ProcessEither = Either[FailedEvent[In], Out]

    val logLeftFlow: Flow[ProcessEither, ProcessEither, NotUsed] =
      LogLeftFlow(name)

    val discardLeftFlow = DiscardLeftFlow[FailedEvent[In], Out]()

    val processFlow = EitherFlow[In, Out](f)

    processFlow
      .via(logLeftFlow)
      .via(discardLeftFlow)
  }
}
