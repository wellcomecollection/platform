package uk.ac.wellcome.platform.archive.common.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow

object DiscardLeftFlow {
  def apply[L,R](): Flow[Either[L, R], R, NotUsed] = {
    Flow[Either[L,R]].collect {
      case Right(right) => right
    }
  }
}