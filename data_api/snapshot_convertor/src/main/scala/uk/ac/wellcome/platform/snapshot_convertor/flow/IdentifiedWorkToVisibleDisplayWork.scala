package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.display.models.{AllWorksIncludes, DisplayWork}
import uk.ac.wellcome.models.IdentifiedWork

import scala.concurrent.ExecutionContext

object IdentifiedWorkToVisibleDisplayWork {
  def apply()(implicit executionContext: ExecutionContext): Flow[IdentifiedWork, DisplayWork, NotUsed] = Flow[IdentifiedWork].filter(_.visible)
    .map { DisplayWork(_, includes = AllWorksIncludes()) }
}
