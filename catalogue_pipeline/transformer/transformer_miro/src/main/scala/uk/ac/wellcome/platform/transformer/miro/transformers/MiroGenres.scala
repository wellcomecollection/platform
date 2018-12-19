package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord
import uk.ac.wellcome.models.work.text.TextNormalisation.sentenceCase

trait MiroGenres {
  def getGenres(
    miroRecord: MiroRecord): List[Genre[Unidentifiable[Concept]]] = {
    // Populate the genres field.  This is based on two fields in the XML,
    // <image_phys_format> and <image_lc_genre>.
    (miroRecord.physFormat.toList ++ miroRecord.lcGenre.toList).map { label =>
      val normalisedLabel = sentenceCase(label)
      Genre[Unidentifiable[Concept]](
        label = normalisedLabel,
        concepts = List(Unidentifiable(Concept(normalisedLabel)))
      )
    }.distinct
  }
}
