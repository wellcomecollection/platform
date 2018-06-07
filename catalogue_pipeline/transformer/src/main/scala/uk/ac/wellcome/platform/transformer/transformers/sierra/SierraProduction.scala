package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.exceptions.GracefulFailureException
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
    val maybeMarc260fields = bibData.varFields.filter { _.marcTag == Some("260") }
    val maybeMarc264fields = bibData.varFields.filter { _.marcTag == Some("264") }

    (maybeMarc260fields, maybeMarc264fields) match {
      case (Nil, Nil) => List()
      case (_, Nil) => List()
      case (Nil, _) => List()
      case (_, _) => throw new GracefulFailureException(new RuntimeException(
        "Record has both 260 and 264 fields; this is a cataloguing error."
      ))
    }

    List()
  }
}
