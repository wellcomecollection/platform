package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{
  AbstractAgent,
  MaybeDisplayable,
  ProductionEvent
}
import uk.ac.wellcome.platform.transformer.source.SierraBibData

trait SierraProduction {

  // Populate wwork:production.
  //
  // Information about production can come from two fields in MARC: 260 & 264.
  // At Wellcome, 260 is what was used historically -- 264 is what we're moving
  // towards, using RDA rules.
  //
  // It is theoretically possible for a bib record to have both 260 and 264,
  // but it would be a cataloguing error -- we should reject it, and flag it
  // to the librarians.
  //
  def getProduction(bibData: SierraBibData)
    : List[ProductionEvent[MaybeDisplayable[AbstractAgent]]] = {
//    val marc260fields = bibData.varFields.filter { _.fieldTag == Some("260") }
//    val marc264fields = bibData.varFields.filter { _.fieldTag == Some("264") }
    List()
  }
}
