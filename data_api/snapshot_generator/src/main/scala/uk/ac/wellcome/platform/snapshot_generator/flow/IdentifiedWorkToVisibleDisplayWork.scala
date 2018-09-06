package uk.ac.wellcome.platform.snapshot_generator.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.models.work.internal.IdentifiedWork

object IdentifiedWorkToVisibleDisplayWork {
  def apply[T <: DisplayWork](
    toDisplayWork: IdentifiedWork => T): Flow[IdentifiedWork, T, NotUsed] =
    Flow[IdentifiedWork]
      .map { toDisplayWork(_) }
}
