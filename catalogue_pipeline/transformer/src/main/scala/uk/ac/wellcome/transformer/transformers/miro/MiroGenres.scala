package uk.ac.wellcome.transformer.transformers.miro

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.transformer.source.MiroTransformableData

trait MiroGenres {
  def getGenres(
    miroData: MiroTransformableData): List[Genre[Unidentifiable[Concept]]] = {
    // Populate the genres field.  This is based on two fields in the XML,
    // <image_phys_format> and <image_lc_genre>.
    (miroData.physFormat.toList ++ miroData.lcGenre.toList).map { label =>
      Genre[Unidentifiable[Concept]](
        label = label,
        concepts = List(Unidentifiable(Concept(label)))
      )
    }.distinct
  }
}
