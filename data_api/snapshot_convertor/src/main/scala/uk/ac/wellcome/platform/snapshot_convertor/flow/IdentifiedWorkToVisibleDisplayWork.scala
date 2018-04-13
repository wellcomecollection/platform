package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.display.models.AllWorksIncludes
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.models.IdentifiedWork

import scala.concurrent.ExecutionContext

object IdentifiedWorkToVisibleDisplayWork {
  def apply()(implicit executionContext: ExecutionContext)
    : Flow[IdentifiedWork, DisplayWorkV1, NotUsed] =
    Flow[IdentifiedWork]
      .filter(_.visible)
      .map { DisplayWorkV1(_, includes = AllWorksIncludes()) }
}
