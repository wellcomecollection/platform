package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{AbstractAgent, MaybeDisplayable, ProductionEvent}
import uk.ac.wellcome.platform.transformer.source.SierraBibData

trait SierraProduction {
  def getProduction(bibData: SierraBibData): List[ProductionEvent[MaybeDisplayable[AbstractAgent]]] = {
    List()
  }
}
