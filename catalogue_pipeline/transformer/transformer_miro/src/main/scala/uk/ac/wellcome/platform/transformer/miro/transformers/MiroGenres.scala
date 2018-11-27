package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroGenres {
  def getGenres(
    miroRecord: MiroRecord): List[Genre[Unidentifiable[Concept]]] = {
    // Populate the genres field.  This is based on two fields in the XML,
    // <image_phys_format> and <image_lc_genre>.
    (miroRecord.physFormat.toList ++ miroRecord.lcGenre.toList).map { label =>
      Genre[Unidentifiable[Concept]](
        label = label,
        concepts = List(Unidentifiable(Concept(label)))
      )
    }.distinct
  }
}
