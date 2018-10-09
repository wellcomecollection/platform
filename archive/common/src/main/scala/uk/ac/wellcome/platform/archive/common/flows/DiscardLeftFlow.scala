package uk.ac.wellcome.platform.archive.common.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow

/** This process takes an Either[L, R], and throws away the Left.
  *
  * You should only use this if you still have a single flow -- if you're
  * split the flow somewhere, discarding the Lefts makes it impossible
  * to reconstruct the split-up-work later.
  *
  */
object DiscardLeftFlow {
  def apply[L, R](): Flow[Either[L, R], R, NotUsed] = {
    Flow[Either[L, R]].collect {
      case Right(right) => right
    }
  }
}
