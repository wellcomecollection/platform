package uk.ac.wellcome.transformer.transformers.miro

import uk.ac.wellcome.models.work.internal.{AbstractConcept, Concept, Genre}
import uk.ac.wellcome.transformer.source.MiroTransformableData

trait MiroGenres {
  def getGenres(
    miroData: MiroTransformableData): List[Genre[AbstractConcept]] = {
    // Populate the genres field.  This is based on two fields in the XML,
    // <image_phys_format> and <image_lc_genre>.
    (miroData.physFormat.toList ++ miroData.lcGenre.toList).map { label =>
      Genre[AbstractConcept](
        label = label,
        concepts = List(Concept(label))
      )
    }.distinct
  }
}
