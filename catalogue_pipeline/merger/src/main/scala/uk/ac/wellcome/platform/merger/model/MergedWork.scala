package uk.ac.wellcome.platform.merger.model
import uk.ac.wellcome.models.work.internal.{
  UnidentifiedRedirectedWork,
  UnidentifiedWork
}

case class MergedWork(work: UnidentifiedWork,
                      redirectedWork: UnidentifiedRedirectedWork)
